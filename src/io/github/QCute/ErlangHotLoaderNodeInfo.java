package io.github.QCute;

import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.intellij.erlang.application.ErlangApplicationConfiguration;
import org.intellij.erlang.application.ErlangApplicationRunConfigurationType;
import org.intellij.erlang.console.ErlangConsoleRunConfiguration;
import org.intellij.erlang.console.ErlangConsoleRunConfigurationType;
import org.intellij.erlang.rebar.runner.RebarRunConfiguration;
import org.intellij.erlang.rebar.runner.RebarRunConfigurationType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;

public class ErlangHotLoaderNodeInfo {
    // base path
    private static String path;
    // node
    private int uniqueId = 0;
    private String name = "";
    private String node = "";
    private String cookie = "";

    ErlangHotLoaderNodeInfo(int uniqueId, String name, String node, String cookie) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.node = node;
        this.cookie = cookie;
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public String getNode() {
        return node;
    }

    public String getCookie() {
        return cookie;
    }

    public static ErlangHotLoaderNodeInfo getSelectedConfiguration(Project project) {
        path = project.getBasePath();
        HashMap<Integer, ErlangHotLoaderNodeInfo> map = getNodeList(project);
        int id = RunManager.getInstance(project).getSelectedConfiguration().getConfiguration().getUniqueID();
        return map.get(id);
    }

    public static HashMap<Integer, ErlangHotLoaderNodeInfo> getNodeList(Project project) {
        HashMap<Integer, ErlangHotLoaderNodeInfo> map = new HashMap<>();
        // application
        List <RunConfiguration> applications = RunManager.getInstance(project).getConfigurationsList(ErlangApplicationRunConfigurationType.getInstance());
        applications.stream().map(ErlangHotLoaderNodeInfo::fromApplication).forEach(item -> map.put(item.getUniqueId(), item));
        // console
        List <RunConfiguration> consoles = RunManager.getInstance(project).getConfigurationsList(ErlangConsoleRunConfigurationType.getInstance());
        consoles.stream().map(ErlangHotLoaderNodeInfo::fromConsole).forEach(item -> map.put(item.getUniqueId(), item));
        // rebar
        List <RunConfiguration> rebars = RunManager.getInstance(project).getConfigurationsList(RebarRunConfigurationType.getInstance());
        rebars.stream().map(ErlangHotLoaderNodeInfo::fromRebar).forEach(item -> map.put(item.getUniqueId(), item));
        return map;
    }

    public static ErlangHotLoaderNodeInfo fromApplication(RunConfiguration run) {
        ErlangApplicationConfiguration configuration = (ErlangApplicationConfiguration) run;
        Pair<String, String> pair = getNodeAndCookie(configuration.getErlFlags());
        return new ErlangHotLoaderNodeInfo(configuration.getUniqueID(), configuration.getName(), pair.getFirst(), pair.getSecond());
    }

    public static ErlangHotLoaderNodeInfo fromConsole(RunConfiguration run) {
        ErlangConsoleRunConfiguration configuration = (ErlangConsoleRunConfiguration) run;
        Pair<String, String> pair = getNodeAndCookie(configuration.getConsoleArgs());
        return new ErlangHotLoaderNodeInfo(configuration.getUniqueID(), configuration.getName(), pair.getFirst(), pair.getSecond());
    }

    public static ErlangHotLoaderNodeInfo fromRebar(RunConfiguration run) {
        RebarRunConfiguration configuration = (RebarRunConfiguration) run;
        String node = "";
        String cookie = "";
        try {
            String line = "";
            BufferedReader reader = new BufferedReader(new FileReader(path + "/config/vm.args"));
            while ((line = reader.readLine()) != null) {
                if(line.contains("-sname") || line.contains("-name")) {
                    node = line.split("\\s+")[1];
                } else if (line.contains("-setcookie")) {
                    cookie = line.split("\\s+")[1];
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return new ErlangHotLoaderNodeInfo(configuration.getUniqueID(), configuration.getName(), node, cookie);
    }

    private static Pair<String, String> getNodeAndCookie(String flag) {
        String[] list = flag.split("\\s+");
        String node = "";
        String cookie = "";
        for (int i = 0; i < list.length; ++i) {
            if ((list[i].equals("-sname") || list[i].equals("-name")) && i + 1 < list.length) {
                node = list[i + 1];
            } else if (list[i].equals("-setcookie") && i + 1 < list.length) {
                cookie = list[i + 1];
            }
        }
        return new Pair<>(node, cookie);
    }

}
