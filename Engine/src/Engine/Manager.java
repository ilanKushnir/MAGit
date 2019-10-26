package Engine;

import Engine.Commons.*;
import Engine.ExternalXmlClasses.*;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import puk.team.course.magit.ancestor.finder.AncestorFinder;
import puk.team.course.magit.ancestor.finder.CommitRepresentative;

import javax.jnlp.FileContents;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.modelmbean.XMLParseException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.*;

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

    public Folder buildWorkingCopyTree(Path wcPath) throws IOException {
        File rootFolder = null;

        rootFolder = new File(wcPath.toString());
        if(!rootFolder.exists())
            throw new FileSystemNotFoundException("Illegal Path : " + wcPath.toString());

        Folder treeRoot = (Folder) buildWorkingCopyTreeRec(rootFolder);

        return treeRoot;
    }

    public Folder buildWorkingCopyTree() throws IOException{
        return buildWorkingCopyTree(activeRepository.getRootPath());
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

    public StatusLog[] commit(String commitMessage, Commit otherLastCommit) throws NullPointerException, IOException {
        StatusLog [] log = new StatusLog[2];

        try {
            Path path = activeRepository.getRootPath();
            Commit lastCommit = activeRepository.getHEAD().getCommit();
            Folder wcTree = buildWorkingCopyTree();

            if (lastCommit == null) {
                log[0] = Commit.compareTrees(null, wcTree.getComponents().isEmpty() ? null : wcTree, path, path, true);
            } else {
                log[0] = Commit.compareTrees(lastCommit.getTree(), wcTree.getComponents().isEmpty() ? null : wcTree, path, path, true);
            }
            log[1] = Commit.compareTrees(otherLastCommit.getTree(),wcTree.getComponents().isEmpty() ? null : wcTree, path, path, false);

            Commit newCommit = new Commit(lastCommit, otherLastCommit, this.activeUser, commitMessage, wcTree);
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
            createFileInMagit(wcTree ,path);
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

    public Branch createNewBranch(String newBranchName) throws InstanceAlreadyExistsException, IOException {
        HashSet<Branch> branches = this.activeRepository.getBranches();
        boolean isExist = branches.stream()
                .anyMatch(branch -> branch.getName().equals(newBranchName));
        if (isExist) {
            throw new InstanceAlreadyExistsException("The branch '" + newBranchName + "' alrady exists.");
        }

        Branch newBranch = new Branch(newBranchName, activeRepository.getHEAD().getCommit());
        branches.add(newBranch);
        this.createFileInMagit(newBranch, this.activeRepository.getRootPath());

        return newBranch;
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

            Boolean isRemote = !activeRepository.getCollaborationSource().equals(CollaborationSource.LOCAL);

            // branch files
            Path branchesPath = Paths.get(path.toString() , "branches");
            Path remoteBranchesPath = null;
            Path remoteFilePath = null;
            if(isRemote) {
                remoteBranchesPath = Paths.get(branchesPath.toString(), activeRepository.getName());
                remoteFilePath = Paths.get(path.toString(), "Remote");
                file = new File(remoteBranchesPath.toString());
                file.mkdir();
                Manager.createFile("HEAD", this.activeRepository.getHEAD().getName(), remoteBranchesPath, 0);
                Manager.createFile("Remote", this.activeRepository.getRemotePath().toString(), path, 0);
            }

            for(Branch branch : this.activeRepository.getBranches()){
                String pointedCommitSHA = (branch.getCommit() != null)? branch.getCommit().generateSHA() : "";
                if(branch.getCollaborationSource().equals(CollaborationSource.REMOTE)) {
                    Manager.createFile(branch.getName(), pointedCommitSHA, remoteBranchesPath, 0);
                } else {
                    Manager.createFile(branch.getName(), pointedCommitSHA, branchesPath, 0);
                }
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

    public Repository SwitchRepositoryInHub(String repositoryName) throws IOException, ParseException {
        Path repoPath = Paths.get(Constants.MAGITHUB_FOLDER_PATH + File.separator + activeUser + File.separator + repositoryName);
        switchRepository(repoPath);

        return this.activeRepository;
    }

    public void switchRepository(Path path) throws IOException, FileSystemNotFoundException, ParseException {
        validateMagitLibraryStructure(path);
        buildRepositoryFromMagitLibrary(path);  // builed and sets as active repository
        deployCommitInWC(activeRepository.getHEAD().getCommit(), path);
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
                magitSingleBranch.isIsRemote() ? magitSingleBranch.getName().split("/?\\\\")[1] : magitSingleBranch.getName(),
                commits.get(magitSingleBranch.getPointedCommit().getId()),
                magitSingleBranch.isIsRemote() ? CollaborationSource.REMOTE : CollaborationSource.LOCAL
        );
    }

    public void parseXMLRepository(MagitRepository magitRepository) throws Exception{ //repository -> HEAD (branch) -> recent commit -> tree
        Path rootPath = Paths.get(magitRepository.getLocation());
        String headName = magitRepository.getMagitBranches().getHead();
        HashMap<String, Commit> commits = parseXMLCommitsList(magitRepository);

        HashSet<Branch> branchList = ParseXMLBranchList(magitRepository, commits);
        List<String> remoteBranchesNames = branchList.stream()
                .filter(branch -> branch.getCollaborationSource().equals(CollaborationSource.REMOTE))
                .map(Branch::getName)
                .collect(Collectors.toList());
        for(Branch branch: branchList) {
            if(remoteBranchesNames.contains(branch.getName()) && branch.getCollaborationSource().equals(CollaborationSource.LOCAL)) {
                branch.setCollaborationSource(CollaborationSource.REMOTETRACKING);
            }
        }
        Branch HEAD = branchList.stream()
                .filter(branch -> branch.getName().equals(headName) && !branch.getCollaborationSource().equals(CollaborationSource.REMOTE))
                .findFirst()
                .get();

        if(magitRepository.getMagitRemoteReference() != null && magitRepository.getMagitRemoteReference().getLocation() != null) {   // check if Remote Repository
            Path remotePath = Paths.get(magitRepository.getMagitRemoteReference().getLocation());
            this.activeRepository = new Repository(rootPath, HEAD, branchList, remotePath, CollaborationSource.REMOTE);
        } else {
            this.activeRepository = new Repository(rootPath, HEAD, branchList, null, CollaborationSource.LOCAL);
        }

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

        if(magitRepository.getMagitRemoteReference() != null) {
            validateRemoteRepoisitory(magitRepository, branchesList);
        }
    }

    private void validateRemoteRepoisitory(MagitRepository magitRepository, List<MagitSingleBranch> branchList) throws InstanceNotFoundException {
        MagitRepository.MagitRemoteReference magitRemoteReference = magitRepository.getMagitRemoteReference();
        boolean isRemoteTracked = false;

        for(MagitSingleBranch branch : branchList) {
            if (branch.isIsRemote()) {
                isRemoteTracked = true;
            }
        }

        if (isRemoteTracked) {
            if(magitRemoteReference.getLocation() == null) {
                throw new InstanceNotFoundException("XML Remote Repository path is corrupted or doesnt exist");
            }
            Path remotePath = Paths.get(magitRemoteReference.getLocation());
            try {
                validateMagitLibraryStructure(remotePath);
            }catch (FileSystemNotFoundException e) {
                throw new InstanceNotFoundException("Remote library structure is corrupted");
            }

            for(MagitSingleBranch branch: branchList) {
                if(branch.isTracking()) {
                    MagitSingleBranch remoteBranch = XMLFindMagitBranchById(branchList ,branch.getTrackingAfter());
                    if(remoteBranch == null || !remoteBranch.isIsRemote()) {
                        throw new InstanceNotFoundException("XML Remote Repository is corrupted: cannot find Remote for branch " + branch.getName());
                    }
                }
            }
        }
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

    public void importFromXMLToHub(String xmlFileContent) throws Exception {
        String tempXMLfileName = "tempXML.xml";
        String currActiveUser = this.activeUser;
        String userFolderPath = Constants.MAGITHUB_FOLDER_PATH + File.separator + this.activeUser;
        createFile(tempXMLfileName, xmlFileContent, Paths.get(userFolderPath), 0);

        Path xmlPath = Paths.get(userFolderPath + File.separator + tempXMLfileName);
        try {
            validateXMLPath(xmlPath);
            InputStream inputStream = new FileInputStream(xmlPath.toString());

            MagitRepository magitRepository = deserializeFrom(inputStream);
            File xmlRepositoryMagitPath = new File(userFolderPath + File.separator + ".magit");

            validateXMLRepository(magitRepository);
            magitRepository.setLocation(userFolderPath + File.separator + magitRepository.getName());
            if (xmlRepositoryMagitPath.exists())
                throw new ObjectAlreadyActive(magitRepository.getLocation());
            parseXMLRepository(magitRepository);
            this.activeUser = currActiveUser;
        } catch (JAXBException e) {
        }

        deleteFolder(new File(userFolderPath + File.separator + tempXMLfileName));
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

    private Repository buildRepositoryFromMagitLibrary(Path rootPath, Boolean bool) throws IOException {
        HashSet<Branch> branches = new HashSet<>();
        Branch HEAD = null;
        File branchesFolder = new File(rootPath.toString() + File.separator + ".magit" + File.separator + "branches");
        File headFile = new File(rootPath.toString() + File.separator + ".magit" + File.separator + "branches" + File.separator + "HEAD");
        File[] branchFiles = branchesFolder.listFiles(file -> (!file.isHidden() && !file.getName().equals("HEAD") && !file.isDirectory()));

        File remoteFile = new File(rootPath.toString(), File.separator + ".magit" + File.separator + "Remote");
        boolean isRemote = (remoteFile.exists());


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

        if(isRemote) {   // in case of remote repository load remote branches as well
            String[] repositoryPathString = rootPath.toString().split("/?\\\\");
            String repositoryName = repositoryPathString[repositoryPathString.length - 1];
            File remoteBranchesFolder = new File(rootPath.toString() + File.separator + ".magit" + File.separator + "branches" + File.separator + repositoryName);

            branchFiles = remoteBranchesFolder.listFiles(file -> (!file.isHidden() && !file.getName().equals("HEAD")));
            Path remotePath = Paths.get(readFileToString(remoteFile));
            ArrayList<String> remoteBranchesNames = new ArrayList<>();

            for (File branchFile : branchFiles) {
                Branch remoteBranch = new Branch(branchFile, CollaborationSource.REMOTE);
                branches.add(remoteBranch);
                remoteBranchesNames.add(remoteBranch.getName());
            }

            for(Branch branch: branches) {
                if(remoteBranchesNames.contains(branch.getName()) && branch.getCollaborationSource().equals(CollaborationSource.LOCAL)) {    // the given branch isnt local = make it RTB
                    branch.setCollaborationSource(CollaborationSource.REMOTETRACKING);
                }
            }

            return new Repository(rootPath, HEAD, branches, remotePath, CollaborationSource.REMOTE);
        } else {    // not a Remote handeled repository
            return new Repository(rootPath, HEAD, branches, null, CollaborationSource.LOCAL);
        }
    }

    private void buildRepositoryFromMagitLibrary(Path rootPath) throws IOException {
        activeRepository = buildRepositoryFromMagitLibrary(rootPath, true);
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

        // creating folder sub components
        for(Folder.Component component: folder.getComponents()) {
            if(component.getComponent() instanceof Folder) {
                createFolderZip((Folder)component.getComponent(), path);
            } else { // component is 'Blob' object
                createBlobZip((Blob)component.getComponent(), path);
            }
        }

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

    public static String readFileToString(String pathString) {
        File file = new File(pathString);
        return readFileToString(file);
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


    public LinkedList getCommitsList() throws IOException {     // todo check if sending new hashmap works
        return getCommitsList(new HashMap<>());
    }

    public LinkedList getCommitsList(HashMap<Commit, Integer> commitsIndexes) throws IOException {
        LinkedList<Commit> commitsList = new LinkedList<>();
        int index = 1;

        for (Branch branch : activeRepository.getBranches()) {
            getCommitsListRec(commitsList, commitsIndexes, branch.getCommit().generateSHA(), branch.getName().equals("master")? 0 : index);
            index++;
        }

        Collections.sort(commitsList);
        Collections.reverse(commitsList);
        return removeCommitsListDuplicates(commitsList);
    }

    private LinkedList<Commit> removeCommitsListDuplicates(LinkedList<Commit> commitsList) {
        LinkedList<Commit> newCommitsList = new LinkedList<>();
        if (commitsList.size() > 0) {
            newCommitsList.add(commitsList.get(0));
        }

        for (Commit commit : commitsList) {
            if (!commit.generateSHA().equals(newCommitsList.get(newCommitsList.size()-1).generateSHA())) {
                newCommitsList.add(commit);
            }
        }

        return newCommitsList;
    }


    private void getCommitsListRec(LinkedList<Commit> commitsList, HashMap<Commit, Integer> commitsIndexes, String SHA1, int index) throws IOException {
        if(SHA1.equals("")) {
            return;
        }

        File commitFile = new File(this.activeRepository.getRootPath().toString() + File.separator + ".magit" + File.separator + "objects" + File.separator + SHA1 + ".zip");
        if(!commitFile.exists()) {
            throw new FileNotFoundException("There is a missing commit file in MAGit repository.");
        }

        Commit commit = new Commit(commitFile);
        commitsList.add(commit);
        commitsIndexes.put(commit, commitsIndexes.isEmpty() ?
                index :
                commitsIndexes.get(commit) == null ?
                        index :
                        commitsIndexes.get(commit).equals(0) ?
                                0 :
                                index);



        getCommitsListRec(commitsList, commitsIndexes, commit.getParentCommitSHA(), index);
    }


    /////////////////////////////   MERGE    /////////////////////////////////

    public void mergeUpdateWC(Folder tree, List<MergeConflict> solvedConflicts) throws ParseException, IOException {
        if (solvedConflicts != null) {
            for(MergeConflict conflict: solvedConflicts) {
                Folder containingFolderPtr = conflict.getContainingFolder();
                Folder containingFolder = mergeFindContainingFolderOnTree(tree, containingFolderPtr);   //redundant?
                containingFolder.addComponent(conflict.getResultComponent());
            }
        }

        createFileInMagit(tree, activeRepository.getRootPath());
        deletePathContents(this.activeRepository.getRootPath());
        deployFileInPathRec(tree, this.activeRepository.getRootPath());
    }

    private Folder mergeFindContainingFolderOnTree(Folder tree, Folder containingFolder) {      //redundant?

        if(containingFolder.equals(tree)) {
            return tree;
        }

        for(Folder.Component component: tree.getComponents()) {
            if (((Folder)component.getComponent()).equals(containingFolder)) {
                return tree;
            }
            if(component.getType().equals(FolderType.FOLDER)) {
                return mergeFindContainingFolderOnTree((Folder)component.getComponent(), containingFolder);
            }
        }

        return null;
    }

    public void mergePullRequest(Branch ourBranch, Branch theirsBranch) throws Exception {
        Commit oursCommit = ourBranch.getCommit();
        Commit theirsCommit = theirsBranch.getCommit();
        AncestorFinder ancestorFinder = new AncestorFinder(new Function<String, CommitRepresentative>() {
            @Override
            public CommitRepresentative apply(String s) {
                Path commitPath = Paths.get(activeRepository.getRootPath().toString(),".magit", "objects", s + ".zip");
                File file = new File(commitPath.toString());
                try {
                    return (CommitRepresentative) new Commit(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        String ancestorSHA1 = ancestorFinder.traceAncestor(oursCommit.generateSHA(), theirsCommit.generateSHA());
        File file = new File(Paths.get(activeRepository.getRootPath().toString(), ".magit", "objects", ancestorSHA1 + ".zip").toString());
//        Commit ancestorCommit = new Commit(file);

        if(ancestorSHA1.equals(oursCommit.generateSHA())) {  // Fasting forward to TheirsCommit!
            activeRepository.getHEAD().setLastCommit(theirsCommit);
            createFileInMagit(activeRepository.getHEAD(), activeRepository.getRootPath());
        } else {
            throw new Exception("Pull request denied because target branch doesn't contains base branch");
        }

        //mergeRec(ancestorCommit.getTree(), oursCommit.getTree(), theirsCommit.getTree(), resultTree, conflicts);
    }

    public void merge(Branch theirsBranch, Folder resultTree, List<MergeConflict> conflicts) throws IOException, FileNotFoundException {
        Commit oursCommit = this.activeRepository.getHEAD().getCommit();
        Commit theirsCommit = theirsBranch.getCommit();
        AncestorFinder ancestorFinder = new AncestorFinder(new Function<String, CommitRepresentative>() {
            @Override
            public CommitRepresentative apply(String s) {
                Path commitPath = Paths.get(activeRepository.getRootPath().toString(),".magit", "objects", s + ".zip");
                File file = new File(commitPath.toString());
                try {
                    return (CommitRepresentative) new Commit(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        String ancestorSHA1 = ancestorFinder.traceAncestor(oursCommit.generateSHA(), theirsCommit.generateSHA());
        File file = new File(Paths.get(activeRepository.getRootPath().toString(), ".magit", "objects", ancestorSHA1 + ".zip").toString());
        Commit ancestorCommit = new Commit(file);

        // Fast Forward Merge
        if(ancestorSHA1.equals(theirsCommit.generateSHA())){    // no action needed - head allready pointing on Ours Commit
            throw new WriteAbortedException("Head branch already contains branch " + theirsBranch.getName(), null);
        } else if(ancestorSHA1.equals(oursCommit.generateSHA())) {  // Fasting forward to TheirsCommit!
            activeRepository.getHEAD().setLastCommit(theirsCommit);
            createFileInMagit(activeRepository.getHEAD(), activeRepository.getRootPath());
            throw new WriteAbortedException("Fast Forward Merge occurred. \nHead branch last commit is now set to " + theirsBranch.getName() + " most recent Commit", null);
        }

        mergeRec(ancestorCommit.getTree(), oursCommit.getTree(), theirsCommit.getTree(), resultTree, conflicts);
    }

    private void mergeRec(Folder ancestorTree, Folder oursTree, Folder theirsTree, Folder resultTree, List<MergeConflict> conflicts) {
        /*  Conditions:
                1. exist on Ancestor - V
                    1.1 - exist everywhere
                        1.1.1 - A = O = T         (V,V,V)
                        1.1.2 - A = O != T        (V,V,U)
                        1.1.3 - A = T != O        (V,U,V)
                        1.1.4 - A != O = T        (V,U,U)
                        1.1.5 - A != O != T       (V,U1,U2)
                    1.2 - Ancestor & Ours
                        1.2.1 - A = O             (V,V,X)
                        1.2.1 - A != O            (V,U,X)
                    1.3 - Ancestor & Theirs
                        1.3.1 - A = T             (V,X,V)
                        1.3.2 - A != T            (V,X,U)
                    1.4 - Ancestor                (V,X,X)

                2. doesnt exist on Ancestor - X
                    2.1 - exist on both
                        2.1.1 - Ours = Theirs     (X,V,V)
                        2.1.2 - Ours != Theirs    (X,V,U)
                    2.2 - Ours                    (X,V,X)
                    2.3 = Theirs                  (X,X,V)
         */
        LinkedList<Folder.Component> ancestorComponents = ancestorTree == null ? new LinkedList<>() : ancestorTree.getComponents();
        LinkedList<Folder.Component> oursComponents = oursTree == null ? new LinkedList<>() : oursTree.getComponents();
        LinkedList<Folder.Component> theirsComponents = theirsTree == null ? new LinkedList<>() : theirsTree.getComponents();
        int [] compareRes = new int[3], iterators = new int[3];
        Arrays.fill(iterators, 0);
        int o = 0, t = 0, a = 0;    //  o = ours, t = theirs, a = ancestor
        int compareAO, compareAT, compareOT;

        while( a < ancestorComponents.size() && o < oursComponents.size() && t < theirsComponents.size()) {
            mergeCompareComponentsName(compareRes, ancestorComponents.get(a), oursComponents.get(o), theirsComponents.get(t));
            compareAO = compareRes[MergeComparison.AO.ordinal()];
            compareAT = compareRes[MergeComparison.AT.ordinal()];
            compareOT = compareRes[MergeComparison.OT.ordinal()];

            if(compareAO >= 0 || compareAT >= 0 ) { // #1 - file exist on Ancestor - V
                if(compareAO == 0 && compareAT == 0 && compareOT == 0) {    // #1.1 - exist everywhere
                    mergeCheckComponentTypes(ancestorComponents.get(a), oursComponents.get(o), theirsComponents.get(t), MergeComparison.ALL, iterators, resultTree, conflicts);
                } else if(compareAO == 0) {
                    if(compareAT < 0) { //  #1.2 - A & O
                        mergeCheckComponentTypes(ancestorComponents.get(a), oursComponents.get(o), null, MergeComparison.AO, iterators, resultTree, conflicts);
                    } else {    // #2.3 - Theirs (X,X,V)
                        resultTree.addComponent(theirsComponents.get(t++));
                    }
                } else if (compareAT == 0) {
                    if(compareAO < 0) { // #1.3 - A & T
                        mergeCheckComponentTypes(ancestorComponents.get(a), null, theirsComponents.get(t), MergeComparison.AT, iterators, resultTree, conflicts);
                    } else {    // #2.2 - Ours (X,V,X)
                        resultTree.addComponent(oursComponents.get(o++));
                    }
                } else {//else if (compareAO != 0 && compareAT != 0 && compareOT != 0) {    // #1.4 - exist on Ancestor only
                    a++;  //FILE DELETED - NO ACTION NEEDED
                }
            } else {    // #2 - file doesnt exist on Ancestor - X
                if(compareOT == 0) {    // #2.1 - Ours and theirs is the same file
                    mergeCheckComponentTypes(null, oursComponents.get(o), theirsComponents.get(t), MergeComparison.OT, iterators, resultTree, conflicts);
                } else if ( compareOT < 0 ) { // #2.2 - Ours (X,V,X)
                    resultTree.addComponent(oursComponents.get(o++));
                } else {// #2.3 - Theirs (X,X,V)
                    resultTree.addComponent(theirsComponents.get(t++));
                }
            }

            a = Math.max(a, iterators[MergeObjOwner.ANCESTOR.ordinal()]);   // check if necessary (max)
            o = Math.max(o, iterators[MergeObjOwner.OURS.ordinal()]);
            t = Math.max(t, iterators[MergeObjOwner.THEIRS.ordinal()]);
        }

        if(a == ancestorComponents.size()) {
            while(o < oursComponents.size() && t < theirsComponents.size()) {
                mergeCheckComponentTypes(null,oursComponents.get(o), theirsComponents.get(t), MergeComparison.OT, iterators, resultTree, conflicts);
                o = Math.max(o, iterators[MergeObjOwner.OURS.ordinal()]);
                t = Math.max(t, iterators[MergeObjOwner.THEIRS.ordinal()]);
            }

        } else if (o == oursComponents.size()) {
            while(a < ancestorComponents.size() && t < theirsComponents.size()) {
                mergeCheckComponentTypes(ancestorComponents.get(a), null, theirsComponents.get(t), MergeComparison.AT, iterators, resultTree, conflicts);
                a = Math.max(a, iterators[MergeObjOwner.ANCESTOR.ordinal()]);   // check if necessary (max)
                t = Math.max(t, iterators[MergeObjOwner.THEIRS.ordinal()]);
            }

        } else if (t == theirsComponents.size()) {
            while(a < ancestorComponents.size() && o < oursComponents.size()) {
                mergeCheckComponentTypes(ancestorComponents.get(a),oursComponents.get(o), null, MergeComparison.AO, iterators, resultTree, conflicts);
                a = Math.max(a, iterators[MergeObjOwner.ANCESTOR.ordinal()]);   // check if necessary (max)
                o = Math.max(o, iterators[MergeObjOwner.OURS.ordinal()]);
            }
        }

        if(a < ancestorComponents.size()) {
            mergeComponentsFromSingleBranch(a, ancestorComponents, MergeObjOwner.ANCESTOR, resultTree);
        } else if (o < oursComponents.size()) {
            mergeComponentsFromSingleBranch(o, oursComponents, MergeObjOwner.OURS, resultTree);
        } else if (t < theirsComponents.size()) {
            mergeComponentsFromSingleBranch(t, theirsComponents, MergeObjOwner.THEIRS, resultTree);
        }
    }

    public void mergeComponentsFromSingleBranch(int iterator, List<Folder.Component> componentList, MergeObjOwner owner, Folder resultTree) {
        while(iterator < componentList.size()) {
            switch (owner) {
                case ANCESTOR:  // ancestor folder deleted #1.4 - (V,X,X)
                                //FILE DELETED - NO ACTION NEEDED
                    break;
                case OURS:      //  ours folder added #2.2 - (X,V,X)
                case THEIRS:    //  theirs folder added #2.3 - (X,X,V)
                    resultTree.addComponent(componentList.get(iterator));
                    break;
                default:
                    break;
            }
            iterator++;
        }
    }

    private void mergeCheckComponentTypes(Folder.Component ancestor, Folder.Component ours, Folder.Component theirs, MergeComparison toCompare, int[] iterators, Folder resultTree, List<MergeConflict> conflicts){
       //   deals with each case where one or more of the components is a folder
        Folder innerTree = new Folder();
        switch (toCompare) {
            case AO:
                if(!ancestor.getType().equals(ours.getType())) {
                    if(ancestor.getType().equals(FolderType.FOLDER)) {  // ancestor folder deleted #1.4 - (V,X,X)
                        //FILE DELETED - NO ACTION NEEDED
                        iterators[MergeObjOwner.ANCESTOR.ordinal()]++;
                    } else if(ours.getType().equals(FolderType.FOLDER)) {   // ours folder added #2.2 - (X,V,X)
                        resultTree.addComponent(ours);
                        iterators[MergeObjOwner.OURS.ordinal()]++;
                    }
                } else if(ancestor.getType().equals(FolderType.FOLDER)) {    // both components are folders
                    mergeRec((Folder) ancestor.getComponent(), (Folder) ours.getComponent(), null, innerTree, conflicts);
                    resultTree.addComponent(ours.getName(),ours.getType(),ours.getLastModifier(), ours.getLastModified(),innerTree);
                    iterators[MergeObjOwner.ANCESTOR.ordinal()]++;
                    iterators[MergeObjOwner.OURS.ordinal()]++;
                } else {    // both components are files
                    mergeCompareFiles(ancestor, ours, null, MergeComparison.AO, resultTree, conflicts);
                    iterators[MergeObjOwner.ANCESTOR.ordinal()]++;
                    iterators[MergeObjOwner.OURS.ordinal()]++;
                }
                break;
            case AT:
                if(!ancestor.getType().equals(theirs.getType())) {
                    if (ancestor.getType().equals(FolderType.FOLDER)) {  // ancestor folder deleted #1.4 - (V,X,X)
                        //FILE DELETED - NO ACTION NEEDED
                        iterators[MergeObjOwner.ANCESTOR.ordinal()]++;
                    } else if (theirs.getType().equals(FolderType.FOLDER)) { //  theirs folder added #2.3 - (X,X,V)
                        resultTree.addComponent(theirs);
                        iterators[MergeObjOwner.THEIRS.ordinal()]++;
                    }
                } else if(ancestor.getType().equals(FolderType.FOLDER)) {    // both components are folders
                    mergeRec((Folder) ancestor.getComponent(), null, (Folder) theirs.getComponent(), innerTree, conflicts);
                    resultTree.addComponent(theirs.getName(),theirs.getType(),theirs.getLastModifier(), theirs.getLastModified(),innerTree);
                    iterators[MergeObjOwner.ANCESTOR.ordinal()]++;
                    iterators[MergeObjOwner.THEIRS.ordinal()]++;
                } else {    // both components are files
                    mergeCompareFiles(ancestor, null, theirs, MergeComparison.AT, resultTree, conflicts);
                    iterators[MergeObjOwner.ANCESTOR.ordinal()]++;
                    iterators[MergeObjOwner.THEIRS.ordinal()]++;
                }
                break;
            case OT:
                if(!ours.getType().equals(theirs.getType())) {
                    if(ours.getType().equals(FolderType.FOLDER)) {  // ours folder added #2.2 - (X,V,X)
                        resultTree.addComponent(ours);
                        iterators[MergeObjOwner.OURS.ordinal()]++;
                    } else if(theirs.getType().equals(FolderType.FOLDER)) { //  theirs folder added #2.3 - (X,X,V)
                        resultTree.addComponent(theirs);
                        iterators[MergeObjOwner.THEIRS.ordinal()]++;
                    }
                } else if(ours.getType().equals(FolderType.FOLDER)) {    // both components are folders
                    mergeRec(null, (Folder) ours.getComponent(), (Folder) theirs.getComponent(), innerTree, conflicts);
                    resultTree.addComponent(ours.getName(),ours.getType(),ours.getLastModifier(), ours.getLastModified(),innerTree);
                    iterators[MergeObjOwner.OURS.ordinal()]++;
                    iterators[MergeObjOwner.THEIRS.ordinal()]++;
                } else {    // both components are files
                    mergeCompareFiles(null, ours, theirs, MergeComparison.OT, resultTree, conflicts);
                    iterators[MergeObjOwner.OURS.ordinal()]++;
                    iterators[MergeObjOwner.THEIRS.ordinal()]++;
                }
                break;
            case ALL:
                if(!ancestor.getType().equals(ours.getType()) || !ancestor.getType().equals(theirs.getType())) {   // not all components are folders (different objects)
                    if(ancestor.getType().equals(FolderType.FOLDER)) {
                        if(ours.getType().equals(FolderType.FOLDER)) {  // #1.2 - Ancestor & Ours
                            mergeRec((Folder) ancestor.getComponent(), (Folder) ours.getComponent(), null, innerTree, conflicts);
                            resultTree.addComponent(ours.getName(),ours.getType(),ours.getLastModifier(), ours.getLastModified(),innerTree);
                            iterators[MergeObjOwner.ANCESTOR.ordinal()]++;
                            iterators[MergeObjOwner.OURS.ordinal()]++;
                        } else if(theirs.getType().equals(FolderType.FOLDER)){  // #1.3 - Ancestor & theirs
                            mergeRec((Folder) ancestor.getComponent(), null, (Folder) theirs.getComponent(), innerTree, conflicts);
                            resultTree.addComponent(theirs.getName(),theirs.getType(),theirs.getLastModifier(), theirs.getLastModified(),innerTree);
                            iterators[MergeObjOwner.ANCESTOR.ordinal()]++;
                            iterators[MergeObjOwner.THEIRS.ordinal()]++;
                        }  // else --> only ancestor is folder - NO ACTION NEEDED #1.4 - (V,X,X)
                    } else {    // ancestor is not a folder - recursive call with Ours & Theirs
                            mergeCheckComponentTypes(ancestor, ours, theirs, MergeComparison.OT, iterators, resultTree, conflicts);
                    }
                } else if(ancestor.getType().equals(FolderType.FOLDER)){    // all components are folders
                    mergeRec((Folder)ancestor.getComponent(), (Folder)ours.getComponent(), (Folder)theirs.getComponent(), innerTree, conflicts);
                    resultTree.addComponent(ours.getName(),ours.getType(),ours.getLastModifier(), ours.getLastModified(),innerTree);
                    iterators[MergeObjOwner.ANCESTOR.ordinal()]++;
                    iterators[MergeObjOwner.OURS.ordinal()]++;
                    iterators[MergeObjOwner.THEIRS.ordinal()]++;
                } else {    // all components are files #1.1 - exist everywhere
                    mergeCompareFiles(ancestor, ours, theirs, MergeComparison.ALL, resultTree, conflicts);
                    iterators[MergeObjOwner.ANCESTOR.ordinal()]++;
                    iterators[MergeObjOwner.OURS.ordinal()]++;
                    iterators[MergeObjOwner.THEIRS.ordinal()]++;
                }
                break;
            default:
                break;

        }
    }

    private void mergeCompareFiles(Folder.Component ancestor, Folder.Component ours, Folder.Component theirs, MergeComparison toCompare, Folder resultTree, List<MergeConflict> conflicts) {
        //  all table case except: #1.4 (V,X,X), #2.2 (X,V,X), #2.3 (X,X,V)
        if(mergeCompareFilesSHA(ancestor, ours, theirs, toCompare)) {    // files SHA equals
            switch (toCompare) {
                case AO:    //  #1.2.1 - (V,V,X) wc - t
                    resultTree.addComponent(theirs);
                    break;

                case AT:    //   #1.3.1 - (V,X,V) wc - o
                case OT:    //   #2.1.1 - (X,V,V) wc - o
                case ALL:   //   #1.1.1 - (V,V,V) wc - o
                    resultTree.addComponent(ours);
                    break;
                default:
                    break;
            }
        } else {            //  files are differ
            switch (toCompare) {
                case AO:    //  #1.2.1 - (V,U,X) conflict (o / t)
                    conflicts.add(new MergeConflict(ancestor, ours, null, resultTree));
                    break;
                case AT:    //  #1.3.2 - (V,X,U) conflict (o / t)
                    conflicts.add(new MergeConflict(ancestor, null, theirs, resultTree));
                    break;
                case OT:    //  #2.1.2 - (X,V,U) conflict (o / t)
                    conflicts.add(new MergeConflict(null, ours,  theirs, resultTree));
                    break;
                case ALL:   // #1.1 exist everywhere
                    if(mergeCompareFilesSHA(ancestor, ours, theirs, MergeComparison.AO)) {           //  #1.1.2 - (V,V,U) wc - t
                        resultTree.addComponent(theirs);
                    } else if (mergeCompareFilesSHA(ancestor, ours, theirs, MergeComparison.AT)) {   //  #1.1.3 - (V,U,V) wc - o
                        resultTree.addComponent(ours);
                    } else if (mergeCompareFilesSHA(ancestor, ours, theirs, MergeComparison.OT)) {   //  #1.1.4 - (V,U,U) wc - o
                        resultTree.addComponent(ours);
                    } else if (!mergeCompareFilesSHA(ancestor, ours, theirs, MergeComparison.AO) &&
                            !mergeCompareFilesSHA(ancestor, ours, theirs, MergeComparison.OT)) {       //  #1.1.5 - (V,U1,U2) conflict (o / t)
                        conflicts.add(new MergeConflict(ancestor, ours, theirs, resultTree));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private boolean mergeCompareFilesSHA(Folder.Component ancestor, Folder.Component ours, Folder.Component theirs, MergeComparison toCompare) {
        // compares 2 or 3 components SHA.
        switch (toCompare) {
            case AO:
                return ancestor.getSHA().equals(ours.getSHA());
            case AT:
                 return ancestor.getSHA().equals(theirs.getSHA());
            case OT:
                return ours.getSHA().equals(theirs.getSHA());
            case ALL:
                return ancestor.getSHA().equals(theirs.getSHA()) && ancestor.getSHA().equals(ours.getSHA());
            default:
                return false;   // check if debugging reaches this point
        }
    }

    private void mergeCompareComponentsName(int[] compareRes, Folder.Component ancestor, Folder.Component ours, Folder.Component theirs) {
        if(ancestor != null && ours != null) {
            compareRes[MergeComparison.AO.ordinal()] = ancestor.getName().compareTo(ours.getName());
        }
        if(ancestor != null && theirs != null) {
            compareRes[MergeComparison.AT.ordinal()] = ancestor.getName().compareTo(theirs.getName());
        }
        if(ours != null && theirs != null) {
            compareRes[MergeComparison.OT.ordinal()] = ours.getName().compareTo(theirs.getName());
        }
    }


    /////////////////////////////   COLLABORATION    ///////////////////////////////////


    public void fork(String otherUsername, String repositoryName) throws Exception {
        Path loaclPath = Paths.get(Constants.MAGITHUB_FOLDER_PATH + File.separator + activeUser);
        Path remotePath = Paths.get(Constants.MAGITHUB_FOLDER_PATH + File.separator + otherUsername + File.separator + repositoryName);

        clone(remotePath, loaclPath);
    }

    public void clone(Path remotePath, Path localPath) throws Exception {
        validateMagitLibraryStructure(remotePath);
        cloneFiles(remotePath, localPath);
    }

    private void copyFolderContent(Path source, Path destination) throws IOException {
        File src = new File(source.toString());
        File [] sourceFiles = src.listFiles();

        for(File file: sourceFiles) {
            File dest = new File(destination.toString() + File.separator + file.getName());
            if(!dest.exists()) {
                Files.copy(file.toPath(), dest.toPath(), REPLACE_EXISTING);
            }
        }
    }

    private void cloneFiles(Path remotePath, Path localPath) throws Exception {
        String[] remotePathString = remotePath.toString().split("/?\\\\");
        String remoteName = remotePathString[remotePathString.length - 1];

        HashSet<Branch> branches = new HashSet<>();
        File branchesFolder = new File(remotePath.toString() + File.separator + ".magit" + File.separator + "branches");
        Branch HEAD = null;
        File headFile = new File(remotePath.toString() + File.separator + ".magit" + File.separator + "branches" + File.separator + "HEAD");

        File[] branchFiles = branchesFolder.listFiles(file -> (!file.isHidden() && !file.getName().equals("HEAD")));
        for (File branchFile : branchFiles) {
            branches.add(new Branch(branchFile, CollaborationSource.REMOTE));
        }

        String headBranchName = readFileToString(headFile);
        for (Branch branch : branches) {
            if (branch.getName().equals(headBranchName)) {
                HEAD = new Branch(branch.getName(), branch.getCommit(), CollaborationSource.REMOTETRACKING);
                branches.add(HEAD);
                break;
            }
        }
        if (HEAD == null) {
            throw new FileNotFoundException("HEAD is pointing to non existent branch");
        }

        localPath = Paths.get(localPath.toString(), remoteName);

        this.activeRepository = new Repository(localPath, HEAD, branches, remotePath, CollaborationSource.REMOTE);

        File file = new File(localPath.toString());
        if (!file.mkdir())                                       // if URL already exist
            throw new Exception("Repository '" + remoteName + "' already exist");

        initMAGitLibrary(localPath);
     //   updateRemoteBranchesDirectory(localPath, remoteName);       // may be unneccsary
        String objectsFolder = ".magit" + File.separator + "objects";
        copyFolderContent(Paths.get(remotePath.toString(), objectsFolder), Paths.get(localPath.toString(), objectsFolder));
        deployCommitInWC(this.activeRepository.getHEAD().getCommit(), localPath);
        createFile("Remote", remotePath.toString(), Paths.get(localPath.toString(), ".magit"),0 );
    }

    private void updateRemoteBranchesDirectory(Path path, String remoteName) throws IOException {
        Path branchesPath = Paths.get(path.toString(), ".magit", "branches");
        File folder = new File(branchesPath.toString());
        File nestedFolder = new File(branchesPath.toString() + File.separator + remoteName);
        nestedFolder.mkdir();
        File head = new File(branchesPath + File.separator + "HEAD");
        String headName = readFileToString(head);

        File[] branchFiles = folder.listFiles(file -> (!file.isHidden() && !file.getName().equals("HEAD")));
        Boolean isHeadMoved = false;

        for (File branch : branchFiles) {
            File destFolder = new File(nestedFolder.toString() + File.separator + branch.getName());
            if (branch.getName().equals(headName)) {
                if (!isHeadMoved) {
                    Files.copy(branch.toPath(), destFolder.toPath(), REPLACE_EXISTING);
                    isHeadMoved = true;
                }
            } else {
                branch.renameTo(destFolder);
            }
        }
        File nestedHead = new File(nestedFolder.toPath().toString() + File.separator + "HEAD");
        Files.copy(head.toPath(), nestedHead.toPath());
    }

    public void createRemoteTrackingBranchFromRB(Branch remoteBranch, Boolean shouldCheckOut) throws IOException, ParseException, ObjectAlreadyActive {
        Path RTBPath = Paths.get(activeRepository.getRootPath().toString(),  ".magit", "branches");
        File RTBFile = new File(RTBPath.toString() + File.separator + remoteBranch.getName());
        Path RBPath = Paths.get(RTBPath.toString(), activeRepository.getName());
        File RBFile = new File(RBPath.toString() + File.separator + remoteBranch.getName());

        Branch RTBBranch = new Branch(remoteBranch.getName(), remoteBranch.getCommit(), CollaborationSource.REMOTETRACKING);
        activeRepository.getBranches().add(RTBBranch);
        Files.copy(RBFile.toPath(), RTBFile.toPath(), REPLACE_EXISTING);
        if(shouldCheckOut) {
            checkout(RTBBranch.getName());
        }
    }

    public void fetch() throws IOException{
        HashSet<Branch> branches = activeRepository.getBranches();
        branches = branches.stream()
                .filter(branch -> !branch.getCollaborationSource().equals(CollaborationSource.REMOTE))
                .collect(Collectors.toCollection(HashSet::new));

        String objectsFolder = ".magit" + File.separator + "objects";
        String brancesFolder = ".magit" + File.separator + "branches";

        copyFolderContent(Paths.get(activeRepository.getRemotePath().toString(),objectsFolder), Paths.get(activeRepository.getRootPath().toString(), objectsFolder));
        copyFolderContent(Paths.get(activeRepository.getRemotePath().toString(), brancesFolder), Paths.get(activeRepository.getRootPath().toString(), brancesFolder, activeRepository.getName()));

        File branchesFolder = new File(activeRepository.getRemotePath().toString() + File.separator + brancesFolder);
        File[] branchFiles = branchesFolder.listFiles(file -> (!file.isHidden() && !file.getName().equals("HEAD")));

        for (File branchFile : branchFiles) {
            branches.add(new Branch(branchFile, CollaborationSource.REMOTE));
        }
        activeRepository.setBranches(branches);
    }

    public Branch pull() throws IOException {
        Branch headBranch = activeRepository.getHEAD();
        if(headBranch.getCollaborationSource().equals(CollaborationSource.LOCAL)) {
            throw new IllegalArgumentException("Head branch is not a Remote Tracking Branch so it cannot be pulled from Remote " + activeRepository.getRemotePath().toString());
        }

        String brancesFolder = ".magit" + File.separator + "branches";
        String objectsFolder = ".magit" + File.separator + "objects";

        File branchFile = new File(activeRepository.getRemotePath().toString() + File.separator + brancesFolder + File.separator + headBranch.getName());
        if(!branchFile.exists()) {
            throw  new FileNotFoundException("There was a problem finding the remote branch on the Remote Repository");
        }

        HashSet<Branch> branches = activeRepository.getBranches();
        branches = branches.stream()
                .filter(rb -> !rb.getName().equals(headBranch.getName()) || !rb.getCollaborationSource().equals(CollaborationSource.REMOTE))
                .collect(Collectors.toCollection(HashSet::new));
        Branch remoteBranch = new Branch(branchFile, CollaborationSource.REMOTE);

        branches.add(remoteBranch);
        activeRepository.setBranches(branches);
        File rtbBranchFile = new File(activeRepository.getRootPath().toString() + File.separator + brancesFolder + File.separator + activeRepository.getName() + File.separator + headBranch.getName());
        Files.copy(branchFile.toPath(), rtbBranchFile.toPath() , REPLACE_EXISTING);
        copyFolderContent(Paths.get(activeRepository.getRemotePath().toString(),objectsFolder), Paths.get(activeRepository.getRootPath().toString(), objectsFolder));

        return remoteBranch;
    }

    public void updateHeadAfterPull() {

    }

    public void pushMagithub() throws Exception {
        Branch HEAD = activeRepository.getHEAD();
        if(HEAD.getCollaborationSource().equals(CollaborationSource.REMOTETRACKING)) {
            throw new IllegalStateException("Cannot push rtb branch " + HEAD.getName() + " because it is not a local");
        } else if(!isRemoteWCClean()) {
            throw new IllegalAccessException("Cannot push to remote repository " + activeRepository.getRemotePath() + " \nRepository working copy is not clean");
        }

        createFileInMagit(HEAD,                        activeRepository.getRemotePath());
        createFileInMagit(HEAD.getCommit(),            activeRepository.getRemotePath());
        createFileInMagit(HEAD.getCommit().getTree(),  activeRepository.getRemotePath());

        HEAD.setCollaborationSource(CollaborationSource.REMOTETRACKING);
        Branch remoteBranch = createNewBranch(HEAD.getName());
        remoteBranch.setCollaborationSource(CollaborationSource.REMOTE);
        HashSet<Branch> branches = activeRepository.getBranches();
        branches.add(remoteBranch);
    }

    public void push() throws Exception {
        Branch HEAD = activeRepository.getHEAD();
        if(!HEAD.getCollaborationSource().equals(CollaborationSource.REMOTETRACKING)) {     // change to local only
            throw new IllegalStateException("Cannot push local branch " + HEAD.getName() + " because it is not a remote tracking branch");
        } else if(!isRemoteWCClean()) {
            throw new IllegalAccessException("Cannot push to remote repository " + activeRepository.getRemotePath() + " \nRepository working copy is not clean");
        } else if(!isRBUpToDate()) {
            throw new IllegalAccessException("Cannot push to remote repository " + activeRepository.getRemotePath() + "\nRemote Branch " + HEAD.getName() + " is not up to date");
        }

        HashSet<Branch> branches = activeRepository.getBranches();
        branches = branches.stream()
                .filter(branch -> !branch.getName().equals(HEAD.getName()) || !branch.getCollaborationSource().equals(CollaborationSource.REMOTE))
                .collect(Collectors.toCollection(HashSet::new));
        Branch newBranch = new Branch(HEAD.getName(), HEAD.getCommit(), CollaborationSource.REMOTE);
        branches.add(newBranch);
        activeRepository.setBranches(branches);

        createFileInMagit(newBranch,                        activeRepository.getRemotePath());
        createFileInMagit(newBranch.getCommit(),            activeRepository.getRemotePath());
        createFileInMagit(newBranch.getCommit().getTree(),  activeRepository.getRemotePath());
//        XMLcreateMagitFilesOnDirectoryRec(newBranch.getCommit().getTree(), activeRepository.getRemotePath());
        File remoteHeadFile = new File(activeRepository.getRemotePath().toString() + File.separator + ".magit" + File.separator + "branches" + File.separator + "HEAD");
        if(readFileToString(remoteHeadFile).equals(HEAD.getName())) {   // if the pushed branch is the Head branch for the Remote Repository
            deployCommitInWC(newBranch.getCommit(), activeRepository.getRemotePath());
        }
    }

    private Boolean isRemoteWCClean() throws IOException {
        Folder remoteWCTree = buildWorkingCopyTree(activeRepository.getRemotePath());
        Folder remoteRepositoryTree = buildRepositoryFromMagitLibrary(activeRepository.getRemotePath(), true).getHEAD().getCommit().getTree();
        return Commit.compareTrees(remoteRepositoryTree, remoteWCTree, activeRepository.getRemotePath(), activeRepository.getRemotePath(), false).isEmptyLog();
    }

    private Boolean isRBUpToDate() throws IOException {
        Branch localRemoteBranch = activeRepository.getBranches().stream()
                .filter(branch -> branch.getName().equals(activeRepository.getHEAD().getName()) && branch.getCollaborationSource().equals(CollaborationSource.REMOTE))
                .findAny()
                .get();

        File branchesFolder = new File(activeRepository.getRemotePath().toString() + File.separator + ".magit" + File.separator + "branches");
        File[] branchFiles = branchesFolder.listFiles(file -> (!file.isHidden() && !file.getName().equals("HEAD")));
        HashSet<Branch> branches = new HashSet<>();
        for (File branchFile : branchFiles) {
            branches.add(new Branch(branchFile, CollaborationSource.REMOTE));
        }

        Branch remoteRemoteBranch = branches.stream()
                .filter(branch -> branch.getName().equals(activeRepository.getHEAD().getName()))
                .findFirst()
                .get();

        return localRemoteBranch.getCommit().generateSHA().equals(remoteRemoteBranch.getCommit().generateSHA());
    }







}
