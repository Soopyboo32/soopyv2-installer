import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class InstallerFrame extends JFrame {

    public String minecraftFolder;

    public static String version = "0.0.1";

    private JPanel openPage;

    private URL dataDownloadURL;

    private String latestCtVersion;
    private HashMap<String, String> ctFileToVersion;
    private URL ctDownloadURL;
    private URL soopyv2DownloadURL;
    private URL soopyv2UpdateButtonPatcherDownloadURL;

    public InstallerFrame(){
        super("SoopyV2 Installer v" + version);

        minecraftFolder = System.getenv("APPDATA") + "\\.minecraft";
        try {
            dataDownloadURL = new URL("http://soopymc.my.to/api/soopyv2/installerData.json");
        } catch (MalformedURLException e) {}

        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

        JLabel title = new JLabel("SoopyV2 Installer");
		title.setFont(new Font("Serif", Font.BOLD, 36));
		title.setHorizontalAlignment(JLabel.CENTER);
        title.setVerticalAlignment(JLabel.TOP);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.add(title);

        this.add(new JSeparator());
        
        JPanel loadingPage = new JPanel();
        loadingPage.add(new JLabel("Downloading data..."));

        this.openPage(loadingPage);

        this.setLocationRelativeTo(null);  
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
        this.setVisible(true);  
        this.setResizable(false);

        new Thread(()->{
            this.downloadData();

            this.openPage(this.selectMinecraftFilePanel());
        }).start();
    }

    private void resize(){
        this.update(this.getGraphics());
        this.pack();
        this.setSize(this.getSize().width+100, this.getSize().height+100);
    }

    private void openPage(JPanel page){
        if(openPage != null){
            this.remove(openPage);
        }

        openPage = page;

        this.add(openPage);

        this.resize();
    }

    public JPanel selectMinecraftFilePanel(){
        JPanel panel = new JPanel(new BorderLayout());

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Select your minecraft folder");
		title.setFont(new Font("Serif", Font.PLAIN, 36));
		title.setHorizontalAlignment(JLabel.CENTER);
        title.setVerticalAlignment(JLabel.TOP);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(title);
        JLabel title2 = new JLabel("For windows users the default should be correct!");
		title2.setHorizontalAlignment(JLabel.CENTER);
        title2.setVerticalAlignment(JLabel.TOP);
        title2.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(title2);

        panel.add(titlePanel, BorderLayout.NORTH);
        
        JFileChooser chooser = new JFileChooser(); 
        chooser.setCurrentDirectory(new java.io.File(minecraftFolder));
        chooser.setDialogTitle("Select your minecraft directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        panel.add(chooser);

        JButton selectButton = new JButton("Select Folder");
        selectButton.setSize((int) selectButton.getSize().getWidth(),(int) selectButton.getSize().getHeight()*2);

        selectButton.addActionListener(event -> {
            minecraftFolder = chooser.getCurrentDirectory().toString();
            
            this.openPage(this.mainInstallPanel());
        });

        panel.add(selectButton, BorderLayout.SOUTH);

        return panel;
    }

    public JPanel mainInstallPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Boolean isChattriggersInstalled = this.isChattriggersInstalled();
        String chattriggersVersion = this.getChattriggersVersion();
        Boolean chattriggersVersionIsLatest = this.chattriggersVersionIsLatest();
        Boolean isSoopyV2Installed = this.isSoopyV2Installed();
        Boolean isSoopyV2UpdatePatcherInstalled = this.isSoopyV2UpdatePatcherInstalled();


        //Start of install Chattriggers
        JPanel ctInstallPanel = new JPanel();

        JLabel ctInstalledLabel = new JLabel(isChattriggersInstalled?"Chattriggers: Installed (version " + chattriggersVersion + ")":"Chattriggers not installed");

        ctInstallPanel.add(ctInstalledLabel, BorderLayout.WEST);

        if(!isChattriggersInstalled || !chattriggersVersionIsLatest){
            JButton installCtButton = new JButton(isChattriggersInstalled?"Update":"Install");

            installCtButton.addActionListener(event -> {
                new Thread(()->{
                    this.installChattriggers();
                    this.openPage(mainInstallPanel());
                }).start();
            });

            ctInstallPanel.add(installCtButton, BorderLayout.EAST);
        }
        panel.add(ctInstallPanel);
        //End of install Chattriggers
        
        //Start of install SoopyV2
        JPanel soopyV2InstallPanel = new JPanel();

        JLabel soopyV2InstallLabel = new JLabel(isSoopyV2Installed?"SoopyV2: Installed":"SoopyV2: Not Installed");

        soopyV2InstallPanel.add(soopyV2InstallLabel, BorderLayout.WEST);

        if(!isSoopyV2Installed){
            JButton installSoopyV2Button = new JButton("Install");

            installSoopyV2Button.addActionListener(event -> {
                new Thread(()->{
                    this.installSoopyV2();
                    this.openPage(mainInstallPanel());
                }).start();
            });

            soopyV2InstallPanel.add(installSoopyV2Button, BorderLayout.EAST);
        }
        panel.add(soopyV2InstallPanel);
        //End of install SoopyV2

        //Start of install SoopyV2UpdateButtonPatcher
        JPanel soopyV2UpdateButtonPatcherInstallPanel = new JPanel();

        JLabel soopyV2UpdateButtonPatcherInstallLabel = new JLabel(isSoopyV2UpdatePatcherInstalled?"SoopyV2UpdateButtonPatcher: Installed":"SoopyV2UpdateButtonPatcher: Not Installed");

        soopyV2UpdateButtonPatcherInstallPanel.add(soopyV2UpdateButtonPatcherInstallLabel, BorderLayout.WEST);

        if(!isSoopyV2UpdatePatcherInstalled){
            JButton installSoopyV2UpdateButtonPatcherButton = new JButton("Install");

            installSoopyV2UpdateButtonPatcherButton.addActionListener(event -> {
                new Thread(()->{
                    this.installSoopyV2UpdateButtonPatcher();
                    this.openPage(mainInstallPanel());
                }).start();
            });

            soopyV2UpdateButtonPatcherInstallPanel.add(installSoopyV2UpdateButtonPatcherButton, BorderLayout.EAST);
        }
        panel.add(soopyV2UpdateButtonPatcherInstallPanel);
        //End of install SoopyV2UpdateButtonPatcher

        if(!isChattriggersInstalled || !isSoopyV2Installed || !isSoopyV2UpdatePatcherInstalled){
            JButton installAllButton = new JButton("Install All");

            installAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);

            installAllButton.addActionListener(event -> {
                new Thread(()->{
                    if(!isChattriggersInstalled) this.installChattriggers();
                    if(!isSoopyV2Installed) this.installSoopyV2();
                    if(!isSoopyV2UpdatePatcherInstalled) this.installSoopyV2UpdateButtonPatcher();
                    this.openPage(mainInstallPanel());
                }).start();
            });

            panel.add(installAllButton);
        }


        return panel;
    }

    public void installChattriggers(){
        //todo: 1. Make sure the folder structure exists
        //todo: 2. Remove current version of chattriggers
        //todo: 3. Download latest version of chattriggers

        JPanel loadingPage = new JPanel();

        loadingPage.add(new JLabel("Installing ChatTriggers..."));

        this.openPage(loadingPage);

        try {
            Thread.sleep(1000); //todo: replace this with actual download
        } catch (InterruptedException ignored) {}
    }
    public void installSoopyV2(){
        //todo: 1. Make sure the folder structure exists
        //todo: 2. Remove current version of soopyv2
        //todo: 3. Download latest version of soopyv2

        JPanel loadingPage = new JPanel();

        loadingPage.add(new JLabel("Installing SoopyV2..."));

        this.openPage(loadingPage);

        try {
            Thread.sleep(1000); //todo: replace this with actual download
        } catch (InterruptedException ignored) {}
    }
    public void installSoopyV2UpdateButtonPatcher(){
        //todo: 1. Make sure the folder structure exists
        //todo: 2. Remove current version of soopyv2updatebuttonpatcher
        //todo: 3. Download latest version of soopyv2updatebuttonpatcher

        JPanel loadingPage = new JPanel();

        loadingPage.add(new JLabel("Installing SoopyV2UpdateButtonPatcher..."));

        this.openPage(loadingPage);

        try {
            Thread.sleep(1000); //todo: replace this with actual download
        } catch (InterruptedException ignored) {}
    }

    public Boolean isChattriggersInstalled(){ //TODO: this
        return false;
    }

    public Boolean isSoopyV2Installed(){ //TODO: this
        return false;
    }

    public Boolean isSoopyV2UpdatePatcherInstalled(){ //TODO: this
        return false;
    }

    public String getChattriggersVersion(){ //TODO: this
        return "2.0.4";
    }

    public Boolean chattriggersVersionIsLatest(){ //TODO: this
        return false;
    }

    public void downloadData(){
        try {
            HttpURLConnection conn = (HttpURLConnection) dataDownloadURL.openConnection();

            conn.connect();
            int responsecode = conn.getResponseCode();
            if(responsecode != 200){
                JOptionPane.showMessageDialog(this,
                    "Error loading download urls and latest version data",
                    "Network connection failed",
                    JOptionPane.WARNING_MESSAGE);
                System.exit(1);
            }
            
            Scanner sc = new Scanner(conn.getInputStream());
            String data = "";
            while(sc.hasNext()){
                data+=sc.nextLine();
            }
            sc.close();

            JSONParser parse = new JSONParser();

            JSONObject jobj = (JSONObject) parse.parse(data); 

            String latestVersion = (String) jobj.get("latestInstallerVersion");

            if(!version.equals(latestVersion)){
                JOptionPane.showMessageDialog(this,
                    "This installer is version v" + version + " while the latest version of the installer is version v" + latestVersion,
                    "Installer not up to date",
                    JOptionPane.WARNING_MESSAGE);
            }

            latestCtVersion = (String) jobj.get("latestCtVersion");

            JSONObject ctFileToVersiond = (JSONObject) jobj.get("ctFileToVersion");

            for(Object key : ctFileToVersiond.keySet().toArray()){
                ctFileToVersion.put((String) key, (String) ctFileToVersiond.get((String) key));
            }

            ctDownloadURL = new URL((String) jobj.get("ctDownloadURL"));
            soopyv2DownloadURL = new URL((String) jobj.get("soopyv2DownloadUrl"));
            soopyv2UpdateButtonPatcherDownloadURL = new URL((String) jobj.get("soopyv2UpdateButtonPatcherDownloadUrl"));
        } catch (IOException | ParseException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading download urls and latest version data",
                "Network connection failed",
                JOptionPane.WARNING_MESSAGE);
                e.printStackTrace();
            System.exit(1);
        }
    }
}