package io.github.QCute;


import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier.NOTIFICATION_GROUP;

public class ErlHotLoader extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        ErlHotLoaderPersistentState state = ErlHotLoaderPersistentState.getInstance(project);
        try {
            assert project != null;
            assert editor != null;
            assert state != null;
            // project path
            String basePath = project.getBasePath();
            // save current document
            Document document = editor.getDocument();
            FileDocumentManager manager = FileDocumentManager.getInstance();
            manager.saveDocument(document);
            // get current file
            VirtualFile file = manager.getFile(document);
            assert file != null;
            // build compile command
            StringBuilder flag = new StringBuilder();
            flag.append(state.getSdkPath()).append("/bin/erlc").append(" ");
            if (state.getAddDebugInfo()) flag.append(" +debug_info ");
            flag.append(" -I ").append(state.getIncludePath()).append(" ");
            flag.append(" -o ").append(state.getBeamPath()).append(" ");
            flag.append(state.getCompileOpts()).append(" ");
            flag.append(file.getPath());
            // compile and handle compile result
            String compileResult = run(basePath, flag.toString());
            if (compileResult.trim().isEmpty()) {
                // basename as module name
                String basename = file.getName().substring(0, file.getName().lastIndexOf("."));
                // build load command
                flag.setLength(0);
                flag.append(state.getSdkPath()).append("/bin/erl").append(" ");
                flag.append(" -noinput ");
                flag.append(" -name ").append(makeNode()).append(" ");
                flag.append(" -setcookie ").append(state.getCookie()).append(" ");
                flag.append(" -eval ").append(" \"erlang:display(rpc:call('").append(state.getNode()).append("', int, i, ['").append(basename).append("'])), erlang:halt().\"");
                // load module and handle result
                String loadResult = run(basePath, flag.toString());
                // handle result
                if (loadResult.equals("{module," + basename + "}")) {
                    ok(project, "Hot Load success: " + loadResult);
                } else {
                    error(project, "Hot Load error: " + loadResult);
                }
            } else {
                error(project, "Compile error: " + compileResult);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    public static void ok(Project project, String message) {
        final Notification notification = NOTIFICATION_GROUP.createNotification(message, NotificationType.INFORMATION);
        notification.notify(project);
    }

    public static void error(Project project, String message) {
        final Notification notification = NOTIFICATION_GROUP.createNotification(message, NotificationType.ERROR);
        notification.notify(project);
    }

    public static String run(String path, String command) {
        System.out.println(command);
        try {
            String line;
            StringBuilder result = new StringBuilder();
            // execute command
            Process process = Runtime.getRuntime().exec(command, null, new File(path));
            // read result
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            return result.toString();
        } catch (Exception exception) {
            exception.printStackTrace();
            return "";
        }
    }

    public static String makeNode() throws SocketException {
        String name = randomName();
        String ip = getLocalIp4Address().get().toString();
        return name + "@" + ip.substring(1);
    }

    public static String randomName() {
        // random charset
        String chars = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz";
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            value.append(chars.charAt((int)(Math.random() * 52)));
        }
        return value.toString();
    }

    public static Optional<Inet4Address> getLocalIp4Address() throws SocketException {
        final List<Inet4Address> ipByNi = getLocalIp4AddressFromNetworkInterface();
        if (ipByNi.size() != 1) {
            final Optional<Inet4Address> ipBySocketOpt = getIpBySocket();
            if (ipBySocketOpt.isPresent()) {
                return ipBySocketOpt;
            } else {
                return ipByNi.isEmpty() ? Optional.empty() : Optional.of(ipByNi.get(0));
            }
        }
        return Optional.of(ipByNi.get(0));
    }

    private static Optional<Inet4Address> getIpBySocket() throws SocketException {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            if (socket.getLocalAddress() instanceof Inet4Address) {
                return Optional.of((Inet4Address) socket.getLocalAddress());
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public static List<Inet4Address> getLocalIp4AddressFromNetworkInterface() throws SocketException {
        List<Inet4Address> addresses = new ArrayList<>(1);
        Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface n = (NetworkInterface) interfaces.nextElement();
            if (isValidInterface(n)) {
                Enumeration inetAddresses = n.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress i = (InetAddress) inetAddresses.nextElement();
                    if (isValidAddress(i)) {
                        addresses.add((Inet4Address) i);
                    }
                }
            }
        }
        return addresses;
    }


    private static boolean isValidInterface(NetworkInterface ni) throws SocketException {
        return !ni.isLoopback() && !ni.isPointToPoint() && ni.isUp() && !ni.isVirtual() && (ni.getName().startsWith("eth") || ni.getName().startsWith("ens"));
    }

    private static boolean isValidAddress(InetAddress address) {
        return address instanceof Inet4Address && address.isSiteLocalAddress() && !address.isLoopbackAddress();
    }
}
