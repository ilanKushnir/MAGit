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
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Manager {
    private String activeUser = "Admin";
    private Repository activeRepository;

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

    public void createNewRepository(Path path, String name) throws Exception{
        // create new repository pointed by 'activeRepository' which include the file given path, new "Master" branch and initial commit pointed by HEAD
        File file = null;

        file = new File(path.toString());
        if(!file.exists())
            throw new FileSystemNotFoundException("Illegal Path : " + path.toString());

        path = Paths.get(path.toString(),name);
        file = new File(path.toString());
        if(!file.mkdir())                                       // if URL already exist
            throw new Exception("Repository '" + name + "' already exist");

        activeRepository = new Repository(path, new Branch("master"));
        initMAGitLibrary(path);
    }

    private void initMAGitLibrary(Path path) throws Exception{
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

            // branch files
            Path branchesPath = Paths.get(path.toString() , "branches");
            for(Branch branch : this.activeRepository.getBranches()){
                String pointedCommitSHA = (branch.getCommit() != null)? branch.getCommit().generateSHA() : "";
                Manager.createFile(branch.getName(), pointedCommitSHA, branchesPath);
            }
            Manager.createFile("HEAD", this.activeRepository.getHEAD().getName(), branchesPath);

        }catch (NullPointerException ex) {
            throw new Exception("Bad URL");
        }
    }

    public void switchUser(String newUser) {
        activeUser = newUser;
    }

    public void switchRepository(Path path) throws FileSystemNotFoundException, FileNotFoundException {
        validateMagitLibraryStructure(path);
        buildRepositoryFromMagitLibrary(path);
    }

    private void validateMagitLibraryStructure(Path rootPath) throws FileSystemNotFoundException{
        File rootFolder = new File(rootPath.toString());
        if(!rootFolder.exists())
            throw new FileSystemNotFoundException("Illegal path:" + rootPath.toString());
        File magitFolder = new File(rootPath.toString() + "//.magit");
        if(!magitFolder.exists())
            throw new FileSystemNotFoundException("The given repository wasn't initilized");
        File branchesFolder = new File(rootPath.toString() + "//.magit" + "//branches");
        if(!branchesFolder.exists())
            throw new FileSystemNotFoundException("The given repository wasn't initilized correctly (no 'branches' folder)");
        File objectsFolder = new File(rootPath.toString() + "//.magit" + "//objects");
        if(!objectsFolder.exists())
            throw new FileSystemNotFoundException("The given repository wasn't initilized correctly (no 'objects' folder)");
        File masterBranchFile = new File(rootPath.toString() + "//.magit" + "//branches" + "//master");
        if(!masterBranchFile.exists())
            throw new FileSystemNotFoundException("There is no master branch in the given repository");
        File headFile = new File(rootPath.toString() + "//.magit" + "//branches" + "//HEAD");
        if(!headFile.exists())
            throw new FileSystemNotFoundException("There is no HEAD pointer in the given repository");
    }

    private void buildRepositoryFromMagitLibrary(Path rootPath) throws FileNotFoundException {
        HashSet<Branch> branches = new HashSet<>();
        Branch HEAD = null;
        File branchesFolder = new File(rootPath.toString() + "//.magit//branches");
        File headFile = new File(rootPath.toString() + "//.magit//branches//HEAD");

        File[] branchFiles = branchesFolder.listFiles(file -> (!file.isHidden() && !file.getName().equals("HEAD")));
        for (File branchFile : branchFiles) {
            branches.add(new Branch(branchFile));
        }

        // Point HEAD to the right branch
        String headBranchName = readFileToString(headFile);
        for(Branch branch : branches) {
            if(branch.getName().equals(headBranchName)) {
                HEAD = branch;
                break;
            }
        }
        if (HEAD == null){
            throw new FileNotFoundException("HEAD is pointing to non existent branch");
        }

        this.activeRepository.setRootPath(rootPath);
        this.activeRepository.setBranches(branches);
        this.activeRepository.setHEAD(HEAD);
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
