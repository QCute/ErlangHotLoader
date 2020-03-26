package io.github.QCute;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "ErlHotLoadConfigure")
public class ErlHotLoaderPersistentState implements PersistentStateComponent<ErlHotLoaderPersistentState> {
    public Boolean addDebugInfo = false;
    public String includePath = "";
    public String beamPath = "";
    public String compileOpts = "";
    public String cookie = "";
    public String node = "";
    public String sdkPath = "";

    public ErlHotLoaderPersistentState() {}

    @Nullable
    public static ErlHotLoaderPersistentState getInstance(Project project) {
        return ServiceManager.getService(project, ErlHotLoaderPersistentState.class);
    }

    @Override
    public void loadState(@NotNull ErlHotLoaderPersistentState erlHotLoaderPersistentState) {
        XmlSerializerUtil.copyBean(erlHotLoaderPersistentState, this);
    }

    @Nullable
    @Override
    public ErlHotLoaderPersistentState getState() {
        return this;
    }

    public Boolean getAddDebugInfo() {
        return this.addDebugInfo;
    }

    public void setAddDebugInfo(Boolean addDebugInfo) {
        this.addDebugInfo = addDebugInfo;
    }

    public String getIncludePath() {
        return this.includePath;
    }

    public void setIncludePath(String includePath) {
        this.includePath = includePath;
    }

    public String getBeamPath() {
        return this.beamPath;
    }

    public void setBeamPath(String beamPath) {
        this.beamPath = beamPath;
    }

    public String getCompileOpts() {
        return this.compileOpts;
    }

    public void setCompileOpts(String compileOpts) {
        this.compileOpts = compileOpts;
    }

    public String getCookie() {
        return this.cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getNode() {
        return this.node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getSdkPath() {
        return this.sdkPath;
    }

    public void setSdkPath(String sdkPath) {
        this.sdkPath = sdkPath;
    }

}
