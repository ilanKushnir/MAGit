package Engine;

import java.io.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Manager {
    private String activeUser = "Admin";
    private Repository activeRepository;

//    public StatusLog showStatus(){
//        StatusLog statusLog = new StatusLog();
//        statusLog.build(activeRepository);
////////////////////////////////////////////////////////////////////////////////////////////
//        return statusLog;
//    } Test

    public Folder buildWorkingCopyTree() {
        Path rootPath = activeRepository.getRootPath();
        File rootFolder = null;

        rootFolder = new File(rootPath.toString());
        if(!rootFolder.exists())
            throw new FileSystemNotFoundException("Illegal Path : " + rootPath.toString());

        Folder treeRoot = (Folder) buildWorkingCopyTreeRec(rootFolder);

        return treeRoot;
    }

    private FolderComponent buildWorkingCopyTreeRec(File rootFile) {
        FolderComponent subComponent;

        if (!rootFile.isDirectory()) {
            subComponent = new Blob(rootFile);
        } else {
            File[] children = rootFile.listFiles(file -> !file.isHidden());

            if (children != null) {
                subComponent = new Folder();
                for (File child : children) {
                    Folder.Component component = ((Folder)subComponent).new Component();
                    FolderType type = (child.isDirectory())? FolderType.FOLDER : FolderType.FILE;

                    component.setName(child.getName());
                    component.setType(type);
                    component.setLastModifier(this.activeUser);
                    component.setLastModified(Manager.getFormattedDateString(child.lastModified()));
                    component.setComponent(buildWorkingCopyTreeRec(child));
//
                    String generatedFileContent = (type.equals(FolderType.FOLDER)?
                            ((Folder)component.getComponent()).generateFolderContentString() :
                            ((Blob)component.getComponent()).getContent()
                    );
                    String SHA = Manager.generateSHA1FromString(generatedFileContent);
                    component.setSHA(SHA);

                    ((Folder)subComponent).addComponent(component);
                }
            } else {
                subComponent = null;
            }
        }
        return subComponent;
    }

    public StatusLog commit(String commitMessage) {
        Path path = activeRepository.getRootPath();
        Commit lastCommit = activeRepository.getHEAD().getCommit();
        Folder wcTree = buildWorkingCopyTree();
        StatusLog log;

        if(lastCommit == null) {
            log = Commit.compareTrees(null, wcTree, path, path, true);
        } else {
            log = Commit.compareTrees(lastCommit.getTree(), wcTree, path, path,true);
        }
//TODO check why log is missing files

        Commit newCommit = new Commit(lastCommit, this.activeUser, commitMessage, wcTree);
        activeRepository.getHEAD().setLastCommit(newCommit);

        try {
            createFileInMagit(newCommit, path);
            activeRepository.getHEAD().setLastCommit(newCommit);
            createFileInMagit(activeRepository.getHEAD(), path);
        } catch (IOException e) {
        }

        return log;
    }

    //TODO create branch and head files
    public void createNewRepository(Path path, String name) throws FileSystemNotFoundException, Exception{
        // create new repository pointed by 'activeRepository' which include the file given path, new "Master" branch and initial commit pointed by HEAD
        File file = null;

        try{
            file = new File(path.toString());
            if(!file.exists())
                throw new FileSystemNotFoundException("Illegal Path : " + path.toString());

            path = Paths.get(path.toString(),name);
            file = new File(path.toString());
            if(!file.mkdir())                                       // if URL already exist
                throw new Exception("Repository '" + name + "' already exist");


        }catch (NullPointerException ex) {
            throw new Exception("Bad URL");
        }

        activeRepository = new Repository(path,new Branch("master"));
        initMAGit(path);
    }

    private void initMAGit(Path path) throws FileSystemNotFoundException, Exception{
        File file = null;

        try {
            file = new File(path.toString());
            if(!file.exists())             // check existence of the given path
                throw new FileSystemNotFoundException("Illegal Path" + path.toString());

            path = Paths.get(path.toString() ,".magit");
            file = new File(path.toString());
            if(!file.mkdir())
                throw new Exception("Could not init MAGit because the given repository is allready MAGit handeled");

            file = new File(path.toString() + "//branches");
            if(!file.mkdir())
                throw new Exception("Could not init MAGit because the given repository is allready MAGit handeled");

            file = new File(path.toString() + "//objects");
            if(!file.mkdir())
                throw new Exception("Could not init MAGit because the given repository is allready MAGit handeled");

        }catch (NullPointerException ex) {
            throw new Exception("Bad URL");
        }
    }

    public void switchUser(String newUser) {
        activeUser = newUser;
    }

    // TODO check switchRepository func
    public void switchRepository(Path path) throws FileSystemNotFoundException, Exception {

        try{
            File f = new File(path.toString());
            if(!f.exists())
                throw new FileSystemNotFoundException("Illegal Path" + path.toString());
            f = new File(path.toString() + "//.magit");
            if(!f.exists())
                throw new Exception("The given path " + path.toString() + " exist but it is not a MAGit repository");

            // create HashSet of branches from zip file exist on path/.magit/branches;
            HashSet<Branch> branches = new HashSet<>(); ///////////////////////////////////////////////////////////////////////////////

            // find active branch by checking it on head file
            Branch activeBranch = new Branch("");         /////////////////////////////////////////////////////////////////////////////

            activeRepository = new Repository(path, activeBranch, branches);

        }catch (NullPointerException ex) {
            throw new FileSystemNotFoundException("Illegal Path" + path.toString());
        }
    }

    public void switchBranch(Branch newBranch) {
        activeRepository.swichHEAD(newBranch);

    }

    public static String generateSHA1FromFile(File file) {
        String str = file.toString();
        return generateSHA1FromString(str);
    }

    public static String generateSHA1FromString(String str) {
        String sha1 = org.apache.commons.codec.digest.DigestUtils.sha1Hex(str);
        return sha1;
    }

    public static void createFileInMagit(Object obj, Path rootPath) throws IOException {
        rootPath = Paths.get(rootPath.toString(), ".magit");
        Path objectsPath = Paths.get(rootPath.toString(), "objects");
        Path branchesPath = Paths.get(rootPath.toString(), "branches");

        if (obj instanceof Commit) {
            createCommitZip((Commit)obj, objectsPath);
        } else if (obj instanceof Branch) {
            Branch branch = (Branch)obj;
            createFile(branch.getName(), branch.getCommit().generateSHA(), branchesPath);
        } else if (obj instanceof Folder) {
            createFolderZip((Folder)obj, objectsPath);
        } else if (obj instanceof Blob) {
            createBlobZip((Blob)obj, objectsPath);
        }
    }

    private static void createCommitZip(Commit commit, Path path) throws IOException {
        String content = commit.generateCommitFileContent();
        String SHA = Manager.generateSHA1FromString(content);

        Manager.createZipFile(path, SHA, content);
    }

    private static void createFolderZip(Folder folder, Path path) throws IOException {
        String content = folder.generateFolderContentString();
        String SHA = Manager.generateSHA1FromString(content);

        Manager.createZipFile(path, SHA, content);
    }

    private static void createBlobZip(Blob blob, Path path) throws IOException {
        String content = blob.getContent();
        String SHA = Manager.generateSHA1FromString(content);

        Manager.createZipFile(path, SHA, content);
    }

    private static void createZipFile(Path path, String fileName, String fileContent) throws IOException {
        File f = new File(path + "//" + fileName + ".zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
        ZipEntry e = new ZipEntry(fileName);
        out.putNextEntry(e);

        byte[] data = fileContent.toString().getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();
    }

    public static void createFile(String fileName, String fileContent, Path path) {
        Writer out = null;

        File master = new File(path + "//" + fileName);
        try {
            out = null;

            out = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(master)));
            out.write(fileContent);
        } catch (IOException e) {
        }
        finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static String readFileToString(File file)
    {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines( Paths.get(file.getPath()), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    public static String getCurrentDateString() {
        return getFormattedDateString(new Date());
    }

    public static String getFormattedDateString(Object date) {
        String datePattern = "dd.MM.YYYY-HH:mm:ss:SSS";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
        String formattedDateString = simpleDateFormat.format(date);

        return formattedDateString;
    }
}
