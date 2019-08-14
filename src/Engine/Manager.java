package Engine;

import Engine.ExternalXmlClasses.*;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.modelmbean.XMLParseException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Manager {
    private static Path folderPath;
    private String activeUser = "Admin";
    private Repository activeRepository;

    public String getActiveUser() {
        return this.activeUser;
    }

    public Repository getActiveRepository() {
        return this.activeRepository;
    }

    public String showCommitInfo() throws NullPointerException{
        return activeRepository.getHEAD().getCommit().showCommitInfo(activeRepository.getRootPath());

    }

    public Folder buildWorkingCopyTree() throws IOException{
        Path rootPath = activeRepository.getRootPath();
        File rootFolder = null;

        rootFolder = new File(rootPath.toString());
        if(!rootFolder.exists())
            throw new FileSystemNotFoundException("Illegal Path : " + rootPath.toString());

        Folder treeRoot = (Folder) buildWorkingCopyTreeRec(rootFolder);

        return treeRoot;
    }

    private FolderComponent buildWorkingCopyTreeRec(File rootFile) throws IOException {
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

    public StatusLog commit(String commitMessage) throws NullPointerException, IOException {
        StatusLog log = null;

        try {
            Path path = activeRepository.getRootPath();
            Commit lastCommit = activeRepository.getHEAD().getCommit();
            Folder wcTree = buildWorkingCopyTree();

            if (lastCommit == null) {
                log = Commit.compareTrees(null, wcTree.getComponents().isEmpty() ? null : wcTree, path, path, true);
            } else {
                log = Commit.compareTrees(lastCommit.getTree(), wcTree.getComponents().isEmpty() ? null : wcTree, path, path, true);
            }

            Commit newCommit = new Commit(lastCommit, this.activeUser, commitMessage, wcTree);
            activeRepository.getHEAD().setLastCommit(newCommit);

            createFileInMagit(newCommit, path);
            createFileInMagit(wcTree,path);
            activeRepository.getHEAD().setLastCommit(newCommit);
            createFileInMagit(activeRepository.getHEAD(), path);

        } catch (NullPointerException e) {  //TODO Commit, ShowStatus --> check which addinional method can throw Exception
            throw new NullPointerException("No active Repository set");
        } catch (IOException e) {}

        return log;
    }

    public StatusLog showStatus() throws NullPointerException, IOException{
        StatusLog log = null;

        try {
            Path path = activeRepository.getRootPath();
            Commit lastCommit = activeRepository.getHEAD().getCommit();
            Folder wcTree = buildWorkingCopyTree();
            wcTree = wcTree.getComponents().isEmpty() ? null : wcTree;

            if (lastCommit == null) {
                log = Commit.compareTrees(null, wcTree, path, path, false);
            } else {
                log = Commit.compareTrees(lastCommit.getTree(), wcTree, path, path, false);
            }

        } catch (NullPointerException e) {
            throw new NullPointerException("No active Repository set");
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

    public void createNewBranch(String newBranchName) throws InstanceAlreadyExistsException, IOException {
        HashSet<Branch> branches = this.activeRepository.getBranches();
        boolean isExist = branches.stream()
                .anyMatch(branch -> branch.getName().equals(newBranchName));
        if (isExist) {
            throw new InstanceAlreadyExistsException("The branch '" + newBranchName + "' alrady exists.");
        }

        Branch newBranch = new Branch(newBranchName, activeRepository.getHEAD().getCommit());
        branches.add(newBranch);
        this.createFileInMagit(newBranch, this.activeRepository.getRootPath());
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
                Manager.createFile(branch.getName(), pointedCommitSHA, branchesPath, 0);
            }
            Manager.createFile("HEAD", this.activeRepository.getHEAD().getName(), branchesPath, 0);

        }catch (NullPointerException ex) {
            throw new Exception("Bad URL");
        }
    }

    public void switchUser(String newUser) {
        activeUser = newUser;
    }

    public void switchRepository(Path path) throws IOException {
        validateMagitLibraryStructure(path);
        buildRepositoryFromMagitLibrary(path);
    }

    public Folder parseXMLTree(MagitRepository magitRepository) {
        List<MagitSingleFolder> magitSingleFolder = magitRepository.getMagitFolders().getMagitSingleFolder();
        List<MagitBlob> magitBlobs = magitRepository.getMagitBlobs().getMagitBlob();
        MagitSingleFolder magitRootFolder = magitSingleFolder.stream()
                .filter(MagitSingleFolder::isIsRoot)
                .findFirst()
                .get();

        return parseXMLTreeRec(magitRepository, magitRootFolder);
    }

    public Folder parseXMLTreeRec(MagitRepository magitRepository, MagitSingleFolder magitRoot) {
        List<Item> items = magitRoot.getItems().getItem();
        LinkedList<Folder.Component> componentsList = null;

        for(Item item: items) {
            if(item.getType().equals("blob")) {
                // find magitBlobs in blobs list and make 'Blob' object
//                Blob blob = parseXMLBlob(XMLFindMagitBlobById(magitRepository.getMagitBlobs().getMagitBlob(), item.getId()));

            } else {    // item is "folder"
                // find magitFolder in folders list and send it to recursive call
                Folder folder = parseXMLTreeRec(magitRepository, XMLFindMagitFolderById(magitRepository.getMagitFolders().getMagitSingleFolder(), item.getId()));
            }

            //insert item in the component list
        }


        return new Folder(componentsList);
    }

//    public Blob parseXMLBlob (MagitBlob magitBlob) {
//
//    }

    public MagitSingleCommit XMLFindMagitCommitById(List<MagitSingleCommit> magitSingleCommits, String id) {
        return magitSingleCommits.stream()
                .filter(commit -> commit.getId().equals(id))
                .findFirst()
                .orElseGet(null);
    }

    public MagitSingleFolder XMLFindMagitFolderById(List<MagitSingleFolder> magitSingleFolders, String id) {
        return magitSingleFolders.stream()
                .filter(folder -> folder.getId().equals(id))
                .findFirst()
                .orElseGet(null);
    }

    public MagitBlob XMLFindMagitBlobById(List<MagitBlob> magitBlobs, String id) {
        return magitBlobs.stream()
                .filter(folder -> folder.getId().equals(id))
                .findFirst()
                .orElseGet(null);
    }

    public MagitSingleBranch XMLFindMagitBranchById(List<MagitSingleBranch> magitSingleBranches, String id) {
        return magitSingleBranches.stream()
                .filter(branch -> branch.getName().equals(id))
                .findFirst()
                .orElseGet(null);
    }

    public void parseXMLRepository(MagitRepository magitRepository) { //repository -> HEAD (branch) ->recent commit -> tree
        Path rootPath = Paths.get(magitRepository.getLocation());
        String headName = magitRepository.getMagitBranches().getHead();
        List<MagitSingleBranch> magitSingleBranches = magitRepository.getMagitBranches().getMagitSingleBranch();
        MagitSingleBranch magitHEAD = magitSingleBranches.stream()
                .filter(branch -> branch.getName().equals(headName))
                .findFirst()
                .get();
        List<MagitSingleCommit> magitSingleCommits = magitRepository.getMagitCommits().getMagitSingleCommit();
        MagitSingleCommit magitRecentCommit = magitSingleCommits.get(magitSingleCommits.size() - 1);

        //  active commit --> commits list
        //  tree (root folder) --> all folders and blobs inside
        //  branches, HEAD


        //this.activeRepository = new Repository(rootPath, HEAD, branches);
        //this.activeUser = "";
    }

    // TODO test XML Validations
    public void validateXMLRepository(MagitRepository magitRepository) throws InstanceAlreadyExistsException, XMLParseException, InstanceNotFoundException {
        List<MagitBlob> blobsList = magitRepository.getMagitBlobs().getMagitBlob();
        List<MagitSingleFolder> foldersList = magitRepository.getMagitFolders().getMagitSingleFolder();
        List<MagitSingleCommit> commitsList = magitRepository.getMagitCommits().getMagitSingleCommit();
        List<MagitSingleBranch> branchesList = magitRepository.getMagitBranches().getMagitSingleBranch();

        List<String> blobsIDs = blobsList.stream()
                .map(blob -> new String(blob.getId()))
                .collect(Collectors.toList());
        List<String> foldersIDs = foldersList.stream()
                .map(folder -> new String(folder.getId()))
                .collect(Collectors.toList());
        List<String> commitsIDs = commitsList.stream()
                .map(commit -> new String(commit.getId()))
                .collect(Collectors.toList());
        List<MagitSingleFolder> rootFolders = foldersList.stream()
                .filter(MagitSingleFolder::isIsRoot)
                .collect(Collectors.toList());

        // validate unique ids
        validateUniqueStrings(blobsIDs);
        validateUniqueStrings(foldersIDs);
        validateUniqueStrings(commitsIDs);

        // validate if all referenced items exists and not pointed to themselves
        validateTreeObjectsExistence(rootFolders, foldersList, blobsList);

        // validate commits are pointing to existing root folders
        validateCommitsPointingToRootFolders(commitsList, foldersList);

        // vlidate branches are pointing to existing commits
        validateBranchesPointingToExistingCommits(branchesList, commitsList);

        // validate head is pointing to existing branch
        validateHeadPointingToExistingBranch(magitRepository.getMagitBranches().getHead(), branchesList);
    }

    public void validateUniqueStrings(List<String> idsList) throws InstanceAlreadyExistsException {
        boolean isUnique = true;
        Set<String> uniqueIds = new HashSet<>();

        for(String id : idsList){
            if (uniqueIds.contains(id)){
                isUnique = false;
            }
            uniqueIds.add(id);
        }

        if (!isUnique) {
            throw new InstanceAlreadyExistsException("The ids are not unique");
        }
    }

    public void validateHeadPointingToExistingBranch(String headBranchName, List<MagitSingleBranch> branchesList) throws InstanceNotFoundException {
        boolean foundHeadBranch = branchesList.stream()
                .anyMatch(branch -> branch.getName().equals(headBranchName));
        if(!foundHeadBranch){
            throw new InstanceNotFoundException("Head branch is pointing to a non existing branch");
        }
    }

    public void validateBranchesPointingToExistingCommits(List<MagitSingleBranch> branchesList, List<MagitSingleCommit> commitsList) throws InstanceNotFoundException {
        for (MagitSingleBranch branch : branchesList) {
            if (XMLFindMagitCommitById(commitsList, branch.getPointedCommit().getId()) == null)
                throw new InstanceNotFoundException("The branch '" + branch.getName() + "' is pointing to a non existing commit");
        }
    }

    public void validateCommitsPointingToRootFolders(List<MagitSingleCommit> commitsList,
                                                     List<MagitSingleFolder> foldersList) throws InstanceNotFoundException, XMLParseException {
        MagitSingleFolder pointedFolder;
        for(MagitSingleCommit commit : commitsList) {
            pointedFolder = XMLFindMagitFolderById(foldersList, commit.getRootFolder().getId());
            if (pointedFolder == null)
                throw new InstanceNotFoundException("The commit: " + commit.getId() + " is pointing to a non existing root folder");
            if (pointedFolder.isIsRoot() == false)
                throw new XMLParseException("Commit: " + commit.getId() + " is pointing to a non root folder");
        }
    }

    public void validateTreeObjectsExistence(List<MagitSingleFolder> rootFolders,
                                             List<MagitSingleFolder> foldersList,
                                             List<MagitBlob> blobsList) throws InstanceNotFoundException, XMLParseException {
        for(MagitSingleFolder rootFolder : rootFolders) {
            validateTreeObjectsExistenceRec(rootFolder, foldersList, blobsList);
        }
    }

    public void validateTreeObjectsExistenceRec(MagitSingleFolder folder,
                                                List<MagitSingleFolder> foldersList,
                                                List<MagitBlob> blobsList) throws InstanceNotFoundException, XMLParseException {
        List<Item> items = folder.getItems().getItem();
        String itemType, itemID;
        MagitSingleFolder magitFolder;

        for(Item item : items) {
            itemType = item.getType();
            itemID = item.getId();

             if (itemType.equals("blob")) {
                try {
                    XMLFindMagitBlobById(blobsList, itemID);
                } catch (NullPointerException e) {
                    throw new InstanceNotFoundException("There is no blob with the following id: " + itemID);
                }
            } else if(itemType.equals("folder")) {
                 if (folder.getId().equals(itemID)) {
                     throw new XMLParseException("A folder can't contain itself (id: " + itemID + ")");
                 }
                try {
                    magitFolder = XMLFindMagitFolderById(foldersList, itemID);
                } catch (NullPointerException e) {
                    throw new InstanceNotFoundException("There is no folder with the following id: " + itemID);
                }
                validateTreeObjectsExistenceRec(magitFolder, foldersList, blobsList);
            }
        }
    }

    public void importFromXML(Path xmlPath, boolean overwriteExistingRepository)
            throws
            XMLParseException,
            InstanceAlreadyExistsException,
            InstanceNotFoundException,
            ObjectAlreadyActive {
        try {
            validateXMLPath(xmlPath);
            InputStream inputStream = new FileInputStream(xmlPath.toString());
            MagitRepository magitRepository = deserializeFrom(inputStream);
            if (!overwriteExistingRepository) {
                File xmlRepositoryMagitPath = new File(magitRepository.getLocation() + "//.magit");
                if (xmlRepositoryMagitPath.exists()){
                    throw new ObjectAlreadyActive("There is already an existing repository in the XML's path.");
                }
            }
            validateXMLRepository(magitRepository);
//            parseXMLRepository(magitRepository);
        } catch (JAXBException e) {
        } catch (FileNotFoundException e) {}
    }

    private MagitRepository deserializeFrom(InputStream in) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance("Engine.ExternalXmlClasses");
        Unmarshaller u = jc.createUnmarshaller();
        return (MagitRepository) u.unmarshal(in);
    }

    public void validateXMLPath(Path xmlPath) throws FileNotFoundException {
        String pathString = xmlPath.toString();
        File xmlFile = new File(pathString);
        if (!xmlFile.exists()) {
            throw new FileNotFoundException("There is no XML file in the given path.");
        }
        if (pathString.length() < 4 || pathString.substring(pathString.length() - 4).equals(.xml)) {
            throw new FileNotFoundException("The given path is not a XML file.");
        }
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

    private void buildRepositoryFromMagitLibrary(Path rootPath) throws IOException {
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

        if(this.activeRepository == null) {
            this.activeRepository = new Repository(rootPath, HEAD, branches);
        } else {
            this.activeRepository.setRootPath(rootPath);
            this.activeRepository.setBranches(branches);
            this.activeRepository.setHEAD(HEAD);
        }
    }

    public void switchBranch(Branch newBranch) {
        activeRepository.swichHEAD(newBranch);
    }

    public void checkout(String branchName) throws FileNotFoundException, ParseException, ObjectAlreadyActive {
        Branch checkoutBranch = this.activeRepository.getBranchByName(branchName);
        Path rootPath = this.activeRepository.getRootPath();

        if (this.activeRepository.getHEAD() == checkoutBranch) {
            throw new ObjectAlreadyActive("You are already on '" + checkoutBranch.getName() + "' branch.");
        }

        deletePathContents(rootPath);
        deployCommitInWC(checkoutBranch.getCommit(), rootPath);
        this.activeRepository.setHEAD(checkoutBranch);
    }

    public void deployCommitInWC(Commit commit, Path rootPath) throws FileNotFoundException, ParseException {
        Folder rootFolderObject = commit.getRootFolder();
        File rootFolder = new File(rootPath.toString());
        File[] children = rootFolder.listFiles();

        if (!rootFolder.exists()) {
            throw new FileNotFoundException("Wrong root path.");
        }

        deployFileInPathRec(rootFolderObject, rootPath);
    }

    public void deployFileInPathRec(Folder file, Path path) throws ParseException {
        LinkedList<Folder.Component> innerComponents = file.getComponents();
        FolderType componentType;
        long lastModified = 0;
        Path currCompponentPath;

        for(Folder.Component comp : innerComponents) {
            currCompponentPath = Paths.get(path.toString() + "//" + comp.getName());
            componentType = comp.getType();
            lastModified = getDateFromFormattedDateString(comp.getLastModified()).getTime();

            if (componentType.equals(FolderType.FOLDER)){
                File folder = new File(currCompponentPath.toString());
                folder.setLastModified(lastModified);
                folder.mkdir();
                deployFileInPathRec((Folder)comp.getComponent(), currCompponentPath);
            } else if (componentType.equals(FolderType.FILE)) {
                createFile(comp.getName(), ((Blob)comp.getComponent()).getContent(), path, lastModified);
            }
        }
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
            createFile(branch.getName(), branch.getCommit().generateSHA(), branchesPath, 0);
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

    public static void createFile(String fileName, String fileContent, Path path, long lastModified) {
        Writer out = null;

        File master = new File(path + "//" + fileName);

        if(lastModified != 0){
            master.setLastModified(lastModified);
        }

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

    // TODO finish func 11
    public String generateActiveBranchHistory() {
        StringBuilder historyString = new StringBuilder();
        generateActiveBranchHistoryRec(this.activeRepository.getHEAD().getCommit(), historyString);
        return historyString.toString();
    }

    private void generateActiveBranchHistoryRec(Commit commit, StringBuilder historyString) {
        if(commit != null) {
        }
    }

    public static String readFileToString(File file) {
        StringBuilder content = new StringBuilder();
        String fileName = file.getName();

        if(fileName.length() >= 4 && fileName.substring(fileName.length() - 4).equals(".zip")) {
            try {
                ZipFile zip = new ZipFile(file.getPath().toString());
                for (Enumeration e = zip.entries(); e.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    content.append(getTxtFromZip(zip.getInputStream(entry)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try (Stream<String> stream = Files.lines( Paths.get(file.getPath()), StandardCharsets.UTF_8)) {
                stream.forEach(s -> content.append(s).append("\n"));
                if(content.length() > 0){
                    content.deleteCharAt(content.length()-1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return content.toString();
    }

    public void deletePathContents(Path folderPath) throws FileNotFoundException, FileSystemNotFoundException {
        File folder = new File(folderPath.toString());
        if (!folder.isDirectory()){
            throw new FileNotFoundException("The given path is not a folder.");
        }

        File[] children = folder.listFiles(file -> (!file.getName().equals(".magit") && !file.isHidden()));
        for(File child : children) {
            child.delete();
        }
    }

    private  static StringBuilder getTxtFromZip(InputStream in) {
        StringBuilder out = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        try{
            while ((line = reader.readLine()) != null) {
                out.append(line).append("\n");
            }
            if(out.length() > 0) {
                out.deleteCharAt(out.length() - 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out;
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

    // TODO fix this function
    public static Date getDateFromFormattedDateString(String date) throws ParseException {
        String datePattern = "dd.MM.yyyy-HH:mm:ss:SSS";
        return new SimpleDateFormat(datePattern).parse(date);
    }
}
