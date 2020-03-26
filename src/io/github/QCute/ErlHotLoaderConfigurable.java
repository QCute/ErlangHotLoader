package io.github.QCute;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ErlHotLoaderConfigurable implements SearchableConfigurable {
    public static final String ERLANG_RELATED_TOOLS = "Erlang Hot Load Tools";
    // view
    private JPanel panel;
    private JCheckBox addDebugInfo;
    private TextFieldWithBrowseButton includePathSelector;
    private TextFieldWithBrowseButton beamPathSelector;
    private JTextField compileOpts;
    private JTextField cookie;
    private JTextField node;
    private TextFieldWithBrowseButton sdkPathSelector;
    // data
    private ErlHotLoaderPersistentState state;
    // constructor
    public ErlHotLoaderConfigurable(@NotNull Project project) {
        state = ErlHotLoaderPersistentState.getInstance(project);
        try {
            assert state != null;
            // data view
            setup();
            // browse listener
            includePathSelector.addBrowseFolderListener("Select Erlang Include Path", "", null, FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Select Erlang Include Root"));
            beamPathSelector.addBrowseFolderListener("Select Erlang Beam Path", "", null, FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Select Erlang Beam Root"));
            sdkPathSelector.addBrowseFolderListener("Select Erlang SDK Path", "", null, FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Select Erlang SDK Root"));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @NotNull
    @Override
    public String getId() {
        return ERLANG_RELATED_TOOLS;
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @NonNls
    @Override
    public String getDisplayName() {
        return ERLANG_RELATED_TOOLS;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return panel;
    }

    @Override
    public boolean isModified() {
        return addDebugInfo.isSelected() != state.getAddDebugInfo() || !includePathSelector.getText().equals(state.getIncludePath()) || !beamPathSelector.getText().equals(state.getBeamPath()) || !compileOpts.getText().equals(state.getCompileOpts()) || !cookie.getText().equals(state.getCookie()) || !node.getText().equals(state.getNode()) || !sdkPathSelector.getText().equals(state.getSdkPath());
    }

    @Override
    public void apply() {
        // debug info
        state.setAddDebugInfo(addDebugInfo.isSelected());
        // include
        state.setIncludePath(includePathSelector.getText());
        // beam
        state.setBeamPath(beamPathSelector.getText());
        // opts
        state.setCompileOpts(compileOpts.getText());
        // cookie
        state.setCookie(cookie.getText());
        // node
        state.setNode(node.getText());
        // sdk path
        state.setSdkPath(sdkPathSelector.getText());
    }

    @Override
    public void reset() {
        setup();
    }

    @Override
    public void disposeUIResources() {
    }

    // setup view
    private void setup() {
        // debug info
        addDebugInfo.setSelected(state.getAddDebugInfo());
        // include
        includePathSelector.setText(state.getIncludePath());
        // beam
        beamPathSelector.setText(state.getBeamPath());
        // opts
        compileOpts.setText(state.getCompileOpts());
        // cookie
        cookie.setText(state.getCookie());
        // node
        node.setText(state.getNode());
        // sdk path
        sdkPathSelector.setText(state.getSdkPath());
    }

}
