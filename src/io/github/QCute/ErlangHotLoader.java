package io.github.QCute;


import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.erlang.configuration.ErlangCompilerSettings;
import org.intellij.erlang.jps.model.ErlangIncludeSourceRootType;
import org.intellij.erlang.rebar.settings.RebarSettings;
import org.jetbrains.annotations.NotNull;

import kotlin.Pair;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier.NOTIFICATION_GROUP;

public class ErlangHotLoader extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent event) {
        String name = getCurrentFileName(event);
        if(name == null) {
            event.getPresentation().setEnabled(false);
        } else {
            try {
                ErlangHotLoaderNodeInfo info = ErlangHotLoaderNodeInfo.getSelectedConfiguration(event.getProject());
                event.getPresentation().setText("Hot Load '" + basename(name) + "' to " + info.getName(), false);
            } catch (Exception ignored) {}
        }
    }

    private static String getCurrentFileName(AnActionEvent event) {
        // save current document
        try {
            Document document = event.getData(PlatformDataKeys.EDITOR).getDocument();
            return FileDocumentManager.getInstance().getFile(document).getPath();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        try {
            // save current document
            String file = saveFile(event);
            // sdk
            Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
            // compile
            Pair<String, String> result = compile(project, sdk.getHomePath(), file);
            // handle stdout/stderr
            if (result.getSecond().trim().isEmpty()) {
                // reload
                reload(project, sdk.getHomePath(), file);
            } else {
                error(project, "Compile error: " + result);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static String saveFile(AnActionEvent event) {
        // save current document
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        assert editor != null;
        Document document = editor.getDocument();
        FileDocumentManager manager = FileDocumentManager.getInstance();
        manager.saveDocument(document);
        // get current file
        VirtualFile file = manager.getFile(document);
        assert file != null;
        return file.getPath();
    }

    private Pair<String, String> compile(Project project, String sdkPath, String filePath) {
        // erlang compiler options
        ErlangCompilerSettings erlangCompilerSettings = ErlangCompilerSettings.getInstance(project);
        if (!erlangCompilerSettings.isUseRebarCompilerEnabled()) {
            // module
            Module module = ModuleManager.getInstance(project).findModuleByName(project.getName());
            assert module != null;
            // include path
            String includePath = ModuleRootManager.getInstance(module).getSourceRoots(ErlangIncludeSourceRootType.INSTANCE).stream().map(f -> "-I " + f.getPath()).collect(Collectors.joining(""));
            // output path
            String outputPath = CompilerModuleExtension.getInstance(module).getCompilerOutputPath().getPath();
            // compiler options
            String compileOptions = String.join("", erlangCompilerSettings.getAdditionalErlcArguments());
            // erlc compile
            return compileWithErlc(project.getBasePath(), filePath, sdkPath, erlangCompilerSettings.isAddDebugInfoEnabled(), includePath, outputPath, compileOptions);
        } else {
            String rebarPath = RebarSettings.getInstance(project).getRebarPath();
            return compileWithRebar(project.getBasePath(), sdkPath, rebarPath);
        }
    }

    private Pair<String, String> compileWithErlc(String basePath, String filePath, String sdkPath, Boolean addDebugInfo, String includePath, String outputPath, String compileOptions) {
        // build compile command
        StringBuilder flag = new StringBuilder();
        flag.append(sdkPath).append("/bin/erlc").append(" ");
        if (addDebugInfo) flag.append(" +debug_info ");
        flag.append(includePath).append(" ");
        flag.append(" -o ").append(outputPath).append(" ");
        flag.append(compileOptions).append(" ");
        flag.append(filePath);
        // compile and handle compile result
        return run(basePath, flag.toString());
    }

    private static Pair<String, String> compileWithRebar(String basePath, String sdkPath, String rebarPath) {
        // compile and handle compile result
        return run(basePath, sdkPath + "/bin/escript" + " " + rebarPath + " " + "compile");
    }

    private static void reload(Project project, String sdkPath, String file) throws Exception {
        ErlangHotLoaderNodeInfo info = ErlangHotLoaderNodeInfo.getSelectedConfiguration(project);
        if(info == null) return;
        // basename as module name
        String basename = basename(file);
        // build load command
        StringBuilder flag = new StringBuilder();
        flag.append(sdkPath).append("/bin/erl").append(" ");
        flag.append(" -noinput ");
        flag.append(" -name ").append(makeNode()).append(" ");
        flag.append(" -setcookie ").append(info.getCookie()).append(" ");
        flag.append(" -eval ").append(" \"erlang:display({ok == rpc:call('").append(info.getNode()).append("', int, n, ['").append(basename).append("']), ").append("rpc:call('").append(info.getNode()).append("', int, i, ['").append(basename).append("'])}), erlang:halt().\"");
        // load module and handle result
        Pair<String, String> result = run(project.getBasePath(), flag.toString());
        // handle result
        if (result.getFirst().equals("{true,{module," + basename + "}}")) {
            ok(project, "Hot Load " + info.getName() + " success: " + result.getFirst());
        } else {
            error(project, "Hot Load " + info.getName() + " error: " + result.getFirst());
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

    public static Pair<String, String> run(String path, String command) {
        System.out.println(command);
        try {
            String line;
            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();
            // execute command
            Process process = Runtime.getRuntime().exec(command, null, new File(path));
            // read result
            BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            while ((line = outReader.readLine()) != null) {
                out.append(line);
            }
            outReader.close();
            // stderr
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
            while ((line = errReader.readLine()) != null) {
                err.append(line);
            }
            errReader.close();
            return new Pair<>(out.toString(), err.toString());
        } catch (Exception exception) {
            exception.printStackTrace();
            return new Pair<>("", "");
        }
    }

    private static String basename(String name) {
        name = new File(name).getName();
        return name.substring(0, name.lastIndexOf("."));
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
