package com.github.japanoise;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.stream.Collectors;

class Window extends JFrame {
    private JList<DoomGameFile> iwadList;
    private DragDropList pwadList;
    private JList<DoomGameFile> sourceportList;
    private LinkedList<Path> iwadPaths;
    private LinkedList<Path> pwadPaths;

    Window(String title) {
        super(title);
        iwadPaths = new LinkedList<>();
        pwadPaths = new LinkedList<>();
        iwadList = new JList<>(new DefaultListModel<>());
        iwadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pwadList = new DragDropList();
        pwadList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        sourceportList = new JList<>(new DefaultListModel<>());
        sourceportList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(640, 480));

        try {
            loadConfig();
        } catch (Exception ex) {
            showError(ex.getMessage(), "Config load error");
        }

        addElements();

        setVisible(true);
    }

    private void addElements() {
        getContentPane().add(makeListsPanel(), BorderLayout.CENTER);
        getContentPane().add(makeButtonPanel(), BorderLayout.PAGE_END);
        pack();
    }

    private JPanel makeListsPanel() {
        JPanel ret = new JPanel();
        ret.setLayout(new GridLayout(1,3));
        ret.add(panelForList("Source Ports", sourceportList, "Add Source Port", (actionEvent) -> addSourcePort()));
        ret.add(panelForList("IWADs", iwadList, "Add IWAD search directory", (actionEvent) -> addIwadDir()));
        ret.add(panelForList("PWADs", pwadList, "Add PWAD search directory", (actionEvent) -> addPwadDir()));
        return ret;
    }

    private void addPwadDir() {
        Path path = chooseDirectory("Add a PWAD directory");
        if(path != null) {
            pwadPaths.add(path);
            refreshPwadList();
        }
    }

    private Path chooseDirectory(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        //
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().toPath();
        } else {
            return null;
        }
    }

    private void addIwadDir() {
        Path path = chooseDirectory("Add an IWAD directory");
        if(path != null) {
            iwadPaths.add(path);
            refreshIwadList();
        }
    }

    private void addSourcePort() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Choose a source port");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            ((DefaultListModel<DoomGameFile>) sourceportList.getModel()).addElement(new DoomGameFile(chooser.getSelectedFile().toPath()));
        }
    }

    private JComponent panelForList(String title, JList<DoomGameFile> list, String buttonText, ActionListener buttonAction) {
        Box boxLayout = Box.createVerticalBox();
        boxLayout.setBorder(BorderFactory.createTitledBorder(title));
        JScrollPane scrollPane = new JScrollPane(list);
        boxLayout.add(scrollPane);
        JButton button = new JButton(buttonText);
        button.addActionListener(buttonAction);
        button.setPreferredSize(new Dimension(Short.MAX_VALUE, button.getPreferredSize().height));
        button.setMaximumSize(button.getPreferredSize());
        boxLayout.add(button);
        return boxLayout;
    }

    private JPanel makeButtonPanel() {
        JPanel ret = new JPanel();
        JButton launch = new JButton("Launch!");
        launch.addActionListener((actionEvent)-> launchGame());
        ret.add(launch);
        JButton refresh = new JButton("Refresh file lists");
        refresh.addActionListener(actionEvent -> refreshFileLists());
        ret.add(refresh);
        return ret;
    }

    private void refreshFileLists() {
        refreshPwadList();
        refreshIwadList();
    }

    private void refreshPwadList() {
        refreshFileList(pwadList, pwadPaths);
    }

    private void refreshIwadList() {
        refreshFileList(iwadList, iwadPaths);
    }

    private void refreshFileList(JList<DoomGameFile> list, LinkedList<Path> pathList) {
        ListModel<DoomGameFile> model = list.getModel();
        DefaultListModel<DoomGameFile> dlm = (DefaultListModel<DoomGameFile>) model;
        dlm.removeAllElements();
        for(Path path:pathList) {
            try {
                Files.walk(path, FileVisitOption.FOLLOW_LINKS)
                        .filter(f -> Files.isRegularFile(f))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toUpperCase()))
                        .forEach(p -> dlm.addElement(new DoomGameFile(p)));
            } catch (IOException e) {
                showError(e.getMessage(), "List error");
            }
        }
    }

    private void showError(String body, String title) {
        JOptionPane.showMessageDialog(this,
                body,
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    private boolean canLaunch() {
        return sourceportList.getSelectedValue() != null && iwadList.getSelectedValue() != null;
    }

    private void launchGame() {
        if (canLaunch()) {
            LinkedList<String> command = new LinkedList<>();
            command.add(sourceportList.getSelectedValue().getFilepath().toString());
            command.add("-iwad");
            command.add(iwadList.getSelectedValue().getFilepath().toString());
            for(DoomGameFile pwad : pwadList.getSelectedValuesList()) {
                command.add("-file");
                command.add(pwad.getFilepath().toString());
            }
            System.out.println(command);
            ProcessBuilder pb = new ProcessBuilder(command);
            try {
                saveConfig();
                pb.inheritIO().start();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        command.toString() + " " + e.getMessage(),
                        "Error launching Doom",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadConfig() throws IOException {
        File configFile = getConfigFile();
        if(configFile.exists()) {
            FileInputStream streamIn = new FileInputStream(configFile);
            try (ObjectInputStream objectinputstream = new ObjectInputStream(streamIn)) {
                ConfigContainer container = (ConfigContainer) objectinputstream.readObject();
                pwadPaths = new LinkedList<>(container.pwadPaths.stream().map(path -> Paths.get(path)).collect(Collectors.toList()));
                iwadPaths = new LinkedList<>(container.iwadPaths.stream().map(path -> Paths.get(path)).collect(Collectors.toList()));
                ListModel<DoomGameFile> model = sourceportList.getModel();
                DefaultListModel<DoomGameFile> dlm = (DefaultListModel<DoomGameFile>) model;
                container.sourceports.forEach(path -> dlm.addElement(new DoomGameFile(Paths.get(path))));
                refreshFileLists();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private File getConfigFile() {
        return new File(getDefaultConfigDir() + "config.bin");
    }

    private String getDefaultConfigDir() {
        final String S = System.getProperty("file.separator");
        String rootPath = System.getenv("XDG_CONFIG_HOME");

        if (rootPath == null) {
            rootPath = System.getProperty("user.home") + S + ".config";
        }
        return rootPath+S+"jdoomlauncher"+S;
    }

    private void saveConfig() throws IOException {
        File configFile = getConfigFile();
        configFile.getParentFile().mkdirs();
        ObjectOutputStream oos = null;
        try {
            FileOutputStream fout = new FileOutputStream(configFile);
            oos = new ObjectOutputStream(fout);
            DefaultListModel<DoomGameFile> dlm = (DefaultListModel<DoomGameFile>) sourceportList.getModel();
            oos.writeObject(new ConfigContainer(Arrays.stream(dlm.toArray()).map(o -> (DoomGameFile) o).collect(Collectors.toList()), pwadPaths, iwadPaths));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if(oos != null){
                oos.close();
            }
        }
    }
}
