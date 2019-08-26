package Engine;

import Engine.ExternalXmlClasses.*;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources_sv;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.modelmbean.XMLParseException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
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
            File[] children = rootFile.listFiles(file -> (!file.getName().equals(".magit")));

            if (children.length != 0) {
                subComponent = new Folder();
                for (File child : children) {
                    Folder.Component component = ((Folder)subComponent).new Component();
                    FolderType type = (child.isDirectory())? FolderType.FOLDER : FolderType.FILE;

                    component.setName(child.getName());
                    component.setType(type);
                    component.setLastModifier(this.activeUser);
                    component.setLastModified(Manager.getFormattedDateString(child.lastModified()));
                    component.setComponent(buildWorkingCopyTreeRec(child));

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

        } catch (NullPointerException e) {
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
            wcTree = wcTree == null || wcTree.getComponents().isEmpty() ? null : wcTree;

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
            deletePathContents(path);
            file.mkdir();
            file = new File(path.toString() + File.separator + "branches");
            file.mkdir();
            file = new File(path.toString() + File.separator + "objects");
            file.mkdir();

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

    public void deleteBranch(String branchName) throws ObjectAlreadyActive {
        if (this.activeRepository.getHEAD().getName().equals(branchName)) {
            throw new ObjectAlreadyActive("You can't delete the HEAD branch, please checkout to other branch first.");
        }
        this.activeRepository.removeBranchByName(branchName);
    }

    public String getBranchesListString() {
        StringBuilder branchesListString = new StringBuilder();
        String headBranchName = this.activeRepository.getHEAD().getName();
        Set<Branch> branchesList = this.activeRepository.getBranches();
        for (Branch branch : branchesList) {
            if (branch.getName().equals(headBranchName)) {
                branchesListString.append(" ○ ").append(branch.getName()).append(" [HEAD]").append(System.lineSeparator());
            } else {
                branchesListString.append(" • ").append(branch.getName()).append(System.lineSeparator());
            }
        }

        return branchesListString.toString();
    }

    public void setCommitToHEADBranch(String SHA, Path rootPath) throws IOException {
        File commitFile = new File(rootPath.toString() + File.separator + ".magit" + File.separator + "objects" + File.separator + SHA + ".zip");
        if (!commitFile.exists()) {
            throw new FileNotFoundException("There is no commit with the given SHA.");
        } else {
            this.activeRepository.getHEAD().setLastCommit(new Commit(commitFile));
        }
    }

    public void switchRepository(Path path) throws IOException {
        validateMagitLibraryStructure(path);
        buildRepositoryFromMagitLibrary(path);
    }

    public Folder parseXMLTree(MagitRepository magitRepository) throws ParseException {
        List<MagitSingleFolder> magitSingleFolder = magitRepository.getMagitFolders().getMagitSingleFolder();
        List<MagitBlob> magitBlobs = magitRepository.getMagitBlobs().getMagitBlob();
        MagitSingleFolder magitRootFolder = magitSingleFolder.stream()
                .filter(MagitSingleFolder::isIsRoot)
                .findFirst()
                .get();

        return parseXMLTreeRec(magitRepository, magitRootFolder);
    }

    public Folder parseXMLTreeRec(MagitRepository magitRepository, MagitSingleFolder magitRoot) throws ParseException {
        List<Item> items = magitRoot.getItems().getItem();
        Folder folder = new Folder();

        for(Item item: items) {
            if(item.getType().equals("blob")) {
                MagitBlob magitBlob = XMLFindMagitBlobById(magitRepository.getMagitBlobs().getMagitBlob(), item.getId());
                folder.addComponent(
                        magitBlob.getName(),
                        FolderType.FILE,
                        magitBlob.getLastUpdater(),
                        this.getFormattedDateString(this.getDateFromFormattedDateString(magitBlob.getLastUpdateDate())),
                        new Blob(magitBlob.getContent())
                );
            } else {    // item is "folder"
                MagitSingleFolder magitSingleFolder = XMLFindMagitFolderById(magitRepository.getMagitFolders().getMagitSingleFolder(), item.getId());
                folder.addComponent(
                        magitSingleFolder.getName(),
                        FolderType.FOLDER,
                        magitSingleFolder.getLastUpdater(),
                        this.getFormattedDateString(this.getDateFromFormattedDateString(magitSingleFolder.getLastUpdateDate())),
                        parseXMLTreeRec(magitRepository, magitSingleFolder)
                );
            }

            //insert item in the component list
        }

        return folder;
    }

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

    public HashMap<String, Commit> parseXMLCommitsList(MagitRepository magitRepository) throws ParseException {
        List<MagitSingleCommit> magitCommits = magitRepository.getMagitCommits().getMagitSingleCommit();
//        List<Commit> commits = new LinkedList<>();
        HashMap<String, Commit> commits = new HashMap<>();

        for(MagitSingleCommit magitCommit: magitCommits) {
            commits.put(magitCommit.getId(), parseXMLCommit(magitRepository, magitCommit));
        }

        return commits;
    }

    public Commit parseXMLCommit(MagitRepository magitRepository, MagitSingleCommit magitCommit) throws ParseException {
        if(magitCommit == null) {
            return null;
        }
        MagitSingleCommit magitPrecedingCommit = XMLFindPrecedingCommit(magitRepository, magitCommit);
        MagitSingleFolder magitTree = XMLFindMagitFolderById(magitRepository.getMagitFolders().getMagitSingleFolder(), magitCommit.getRootFolder().getId());

        return new Commit(
                parseXMLCommit(magitRepository, magitPrecedingCommit),
                magitCommit.getAuthor(),
                magitCommit.getMessage(),
                magitCommit.getDateOfCreation(),
                parseXMLTreeRec(magitRepository, magitTree)
        );
    }

    public MagitSingleCommit XMLFindPrecedingCommit(MagitRepository magitRepository, MagitSingleCommit currentCommit) {
        List<MagitSingleCommit> commits = magitRepository.getMagitCommits().getMagitSingleCommit();
        List<PrecedingCommits.PrecedingCommit> precedingCommits =
                currentCommit == null || currentCommit.getPrecedingCommits() == null ?
                        null :
                        currentCommit.getPrecedingCommits().getPrecedingCommit();

        return (precedingCommits == null || precedingCommits.isEmpty()) ?
                null :
                commits.stream()
                .filter(commit -> commit
                        .getId()
                        .equals(precedingCommits
                                .get(0)
                                .getId()))
                .findFirst()
                        .orElseGet(null);
    }

    public HashSet<Branch> ParseXMLBranchList(MagitRepository magitRepository, HashMap<String, Commit> commits) throws ParseException {
        List<MagitSingleBranch> magitSingleBranches = magitRepository.getMagitBranches().getMagitSingleBranch();
        HashSet<Branch> branchList = new HashSet<>();
        for(MagitSingleBranch branch: magitSingleBranches) {
            branchList.add(parseXMLBranch(branch, commits));
        }

        return branchList;
    }

    public Branch parseXMLBranch(MagitSingleBranch magitSingleBranch, HashMap<String, Commit> commits) throws ParseException{
        return new Branch(
                magitSingleBranch.getName(),
                commits.get(magitSingleBranch.getPointedCommit().getId())
        );
//        return new Branch(
//                magitSingleBranch.getName(),
//                parseXMLCommit(
//                        magitRepository,
//                        XMLFindMagitCommitById(
//                                magitRepository.getMagitCommits().getMagitSingleCommit(),
//                                magitSingleBranch.getPointedCommit().getId()
//                        )
//                )
//        );
    }

//    public void parseXMLRepository(MagitRepository magitRepository) throws Exception{ //repository -> HEAD (branch) -> recent commit -> tree
//        Path rootPath = Paths.get(magitRepository.getLocation());
//        String headName = magitRepository.getMagitBranches().getHead();
//        List<MagitSingleBranch> magitSingleBranches = magitRepository.getMagitBranches().getMagitSingleBranch();
//        HashSet<Branch> branchList = ParseXMLBranchList(magitRepository);
//        Branch HEAD = branchList.stream()
//                .filter(branch -> branch.getName().equals(headName))
//                .findFirst()
//                .get();
//
//        List<Commit> commitList = parseXMLCommitsList(magitRepository);
//        this.activeRepository = new Repository(rootPath, HEAD, branchList);
//        this.activeUser = HEAD.getCommit().getAuthor();
//        this.folderPath = rootPath;
//        XMLcreateMagitFilesOnDirectory(commitList, rootPath);
//        deployCommitInWC(HEAD.getCommit(), rootPath);
//    }

    public void parseXMLRepository(MagitRepository magitRepository) throws Exception{ //repository -> HEAD (branch) -> recent commit -> tree
        Path rootPath = Paths.get(magitRepository.getLocation());
        String headName = magitRepository.getMagitBranches().getHead();
        HashMap<String, Commit> commits = parseXMLCommitsList(magitRepository);

        HashSet<Branch> branchList = ParseXMLBranchList(magitRepository, commits);
        Branch HEAD = branchList.stream()
                .filter(branch -> branch.getName().equals(headName))
                .findFirst()
                .get();

        this.activeRepository = new Repository(rootPath, HEAD, branchList);
        this.activeUser = HEAD.getCommit().getAuthor();
        this.folderPath = rootPath;
        XMLcreateMagitFilesOnDirectory(commits, rootPath);
        deployCommitInWC(HEAD.getCommit(), rootPath);
    }

    public void XMLcreateMagitFilesOnDirectory(HashMap<String, Commit> commits, Path path) throws Exception {
            File repository = new File(path.toString());
            repository.mkdir();                             //  creates repository folder
            initMAGitLibrary(path);                         //  create .magit, branches & objects folders, and branches and HEAD files

            for(Commit commit: commits.values()) {
                XMLcreateMagitFilesOnDirectoryRec(commit.getTree(), path);
                createFileInMagit(commit, path);
            }
    }

    public void XMLcreateMagitFilesOnDirectoryRec(Folder rootFolder, Path path) throws Exception{
        List<Folder.Component> components = rootFolder.getComponents();

        for(Folder.Component component: components) {
            if(component.getComponent() instanceof Folder) {
                XMLcreateMagitFilesOnDirectoryRec((Folder)component.getComponent(), path);
                createFileInMagit((Folder)component.getComponent(), path);
            } else { // component is 'Blob' object
                createFileInMagit((Blob)component.getComponent(), path);
            }
        }
        createFileInMagit(rootFolder, path);
    }

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

    public void importFromXML(Path xmlPath, boolean overwriteExistingRepository) throws Exception {
        try {
            validateXMLPath(xmlPath);
            InputStream inputStream = new FileInputStream(xmlPath.toString());
            MagitRepository magitRepository = deserializeFrom(inputStream);
            if (!overwriteExistingRepository) {
                File xmlRepositoryMagitPath = new File(magitRepository.getLocation() + File.separator + ".magit");
                if (xmlRepositoryMagitPath.exists()){
                    throw new ObjectAlreadyActive(magitRepository.getLocation());
                }
            }
            validateXMLRepository(magitRepository);
            parseXMLRepository(magitRepository);
        } catch (JAXBException e) {
        }
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
        if (pathString.length() < 4 || !pathString.substring(pathString.length() - 4).equals(".xml")) {
            throw new FileNotFoundException("The given path is not a XML file.");
        }
    }

    private void validateMagitLibraryStructure(Path rootPath) throws FileSystemNotFoundException{
        File rootFolder = new File(rootPath.toString());
        if(!rootFolder.exists())
            throw new FileSystemNotFoundException("Illegal path:" + rootPath.toString());
        File magitFolder = new File(rootPath.toString() + File.separator + ".magit");
        if(!magitFolder.exists())
            throw new FileSystemNotFoundException("The given repository wasn't initilized");
        File branchesFolder = new File(rootPath.toString() + File.separator + ".magit" + File.separator + "branches");
        if(!branchesFolder.exists())
            throw new FileSystemNotFoundException("The given repository wasn't initilized correctly (no 'branches' folder)");
        File objectsFolder = new File(rootPath.toString() + File.separator + ".magit" + File.separator + "objects");
        if(!objectsFolder.exists())
            throw new FileSystemNotFoundException("The given repository wasn't initilized correctly (no 'objects' folder)");
        File masterBranchFile = new File(rootPath.toString() + File.separator + ".magit" + File.separator + "branches" + File.separator + "master");
        if(!masterBranchFile.exists())
            throw new FileSystemNotFoundException("There is no master branch in the given repository");
        File headFile = new File(rootPath.toString() + File.separator + ".magit" + File.separator + "branches" + File.separator + "HEAD");
        if(!headFile.exists())
            throw new FileSystemNotFoundException("There is no HEAD pointer in the given repository");
    }

    private void buildRepositoryFromMagitLibrary(Path rootPath) throws IOException {
        HashSet<Branch> branches = new HashSet<>();
        Branch HEAD = null;
        File branchesFolder = new File(rootPath.toString() + File.separator + ".magit" + File.separator + "branches");
        File headFile = new File(rootPath.toString() + File.separator + ".magit" + File.separator + "branches" + File.separator + "HEAD");

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
        File[] children = rootFolder.listFiles(file -> !file.getName().equals(".magit"));
        this.deletePathContents(rootPath);

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
            currCompponentPath = Paths.get(path.toString() + File.separator +  comp.getName());
            componentType = comp.getType();
            lastModified = getLongDateFromFormattedDateString(comp.getLastModified());

            if (componentType.equals(FolderType.FOLDER)){
                File folder = new File(currCompponentPath.toString());
                folder.mkdir();
                deployFileInPathRec((Folder)comp.getComponent(), currCompponentPath);
                folder.setLastModified(lastModified);
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
        File f = new File(path + File.separator + fileName + ".zip");
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

        File master = new File(path + File.separator +  fileName);

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

                    if(lastModified != 0){
                        master.setLastModified(lastModified);
                    }
                } catch (IOException e) {
                }
            }
        }
    }

    public String generateActiveBranchHistory() throws FileNotFoundException {
        StringBuilder historyString = new StringBuilder();
        generateActiveBranchHistoryRec(this.activeRepository.getHEAD().getCommit().generateSHA(), historyString);
        return historyString.toString();
    }

    private void generateActiveBranchHistoryRec(String commitSHA, StringBuilder historyString) throws FileNotFoundException {
        if(commitSHA.equals("")) {
            return;
        }

        File commitFile = new File(this.activeRepository.getRootPath().toString() + File.separator + ".magit" + File.separator + "objects" + File.separator + commitSHA + ".zip");

        if(!commitFile.exists()) {
            throw new FileNotFoundException("There is a missing commit file in MAGit repository.");
        }

        String[] commitFileLines = readFileToString(commitFile).split("\\r?\\n");

        historyString.append("SHA-1:          " + commitSHA).append(System.lineSeparator());
        historyString.append("Commit message: " + commitFileLines[2]).append(System.lineSeparator());
        historyString.append("Date created:   " + commitFileLines[3]).append(System.lineSeparator());
        historyString.append("Author:         " + commitFileLines[4]).append(System.lineSeparator());
        historyString.append(System.lineSeparator());

        generateActiveBranchHistoryRec(commitFileLines[0], historyString);
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

        if (folder.exists()) {
            if (!folder.isDirectory()){
                throw new FileNotFoundException("The given path is not a folder.");
            }

            File[] children = folder.listFiles(file -> (!file.getName().equals(".magit")));
            for(File child : children) {
                if (child.isDirectory()) {
                    deleteFolder(child);
                } else {
                    child.delete();
                }
            }
        }
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
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
        String datePattern = "dd.MM.yyyy-HH:mm:ss:SSS";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
        String formattedDateString = simpleDateFormat.format(date);

        return formattedDateString;
    }

    public static long getLongDateFromFormattedDateString(String date) throws ParseException {
        String datePattern = "dd.MM.yyyy-HH:mm:ss:SSS";
        DateFormat dateFormat = new SimpleDateFormat(datePattern);
        long returnVlaue = dateFormat.parse(date).getTime();
        return returnVlaue;
    }

    public static Date getDateFromFormattedDateString(String date) throws ParseException {
        String datePattern = "dd.MM.yyyy-HH:mm:ss:SSS";
        return new SimpleDateFormat(datePattern).parse(date);
    }
}
