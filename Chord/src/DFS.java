import java.rmi.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.math.BigInteger;
import java.security.*;

import com.google.gson.Gson;

import java.io.InputStream;


/* JSON Format

{"file":
  [
     {"name":"MyFile",
      "size":128000000,
      "pages":
      [
         {
            "guid":11,
            "size":64000000
         },
         {
            "guid":13,
            "size":64000000
         }
      ]
      }
   ]
} 
*/


public class DFS {
    Date date = new Date();
    Long metadata;


    public class PagesJson {
        // guid = md5(filename+pagenumber)
        Long guid;
        Long size;

        Long creationTS;
        Long readTS;
        Long writeTS;

        int referenceCount;


        public PagesJson(Long g, Long s) {
            this.guid = g;
            this.size = s;
            this.referenceCount = 0;

            // Set timestamps
            this.creationTS = date.getTime();
            this.readTS = date.getTime();
            this.writeTS = date.getTime();
        }

        // getters
        public Long getGUID() {
            return this.guid;
        }

        public Long getSize() {
            return this.size;
        }

        // setters
        public void setGUID(Long g) {
            this.guid = g;
        }

        public void setSize(Long s) {
            this.size = s;
        }

    }

    ;

    public class FileJson {
        String name;
        Long size;

        Long creationTS;
        Long readTS;
        Long writeTS;

        int numberOfPages;
        int referenceCount;

        ArrayList<PagesJson> pages;

        public FileJson(String n, Long s) {
            this.name = n;
            this.size = s;
            this.numberOfPages = 0;
            this.referenceCount = 0;
            pages = new ArrayList<PagesJson>();

            // Set timestamps
            this.creationTS = date.getTime();
            this.readTS = date.getTime();
            this.writeTS = date.getTime();
        }

        // getters
        public String getName() {
            return this.name;
        }

        public Long getSize() {
            return this.size;
        }

        // setters
        public void setName(String n) {
            this.name = n;
        }

        public void setSize(Long s) {
            this.size = s;
        }

        public void updateNumPages() {
            this.numberOfPages = pages.size();
        }

        public void incrementRef() {
            this.referenceCount++;
        }

        public void decrementRef() {
            this.referenceCount--;
        }

        public void incrementRef(int pageIndex) {
            this.referenceCount++;
            pages.get(pageIndex).referenceCount++;
        }

        public void decrementRef(int pageIndex) {
            this.referenceCount--;
            this.pages.get(pageIndex).referenceCount--;
        }

        public void addPage(PagesJson p, Long size) {
            this.pages.add(p);
            this.size += size;
        }
    }

    ;

    public class FilesJson {
        List<FileJson> file;

        public FilesJson() {
            file = new ArrayList<FileJson>();
        }

        // getters
        public List<FileJson> getFileList() {
            return this.file;
        }

        // setters
        public void addFile(FileJson f) {
            this.file.add(f);
        }
    }

    ;


    int port;
    Chord chord;


    private long md5(String objectName) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(objectName.getBytes());
            BigInteger bigInt = new BigInteger(1, m.digest());
            return Math.abs(bigInt.longValue());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();

        }
        return 0;
    }


    public DFS(int port) throws Exception {


        this.port = port;
        this.metadata = md5("Metadata");
        long guid = md5("" + port);
        chord = new Chord(port, guid);
        Files.createDirectories(Paths.get(guid + "\\repository"));
        Files.createDirectories(Paths.get(guid + "\\tmp"));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                chord.leave();
            }
        });

    }


    /**
     * Join the chord
     */
    public void join(String Ip, int port) throws Exception {
        chord.joinRing(Ip, port);
        chord.print();
    }


    /**
     * leave the chord
     */
    public void leave() throws Exception {
        chord.leave();
    }

    /**
     * print the status of the peer in the chord
     */
    public void print() throws Exception {
        chord.print();
    }

    /**
     * readMetaData read the metadata from the chord
     */
    public FilesJson readMetaData() throws Exception {
        FilesJson filesJson = null;
        long guid = md5("Metadata");
        Gson gson = new Gson();

        // Try to read metadata if it does not exist create a new physical file for it
        try {
            //System.out.println("Read metadata starting");
            ChordMessageInterface peer = chord.locateSuccessor(guid);
            RemoteInputFileStream metadataraw = peer.get(guid);
            metadataraw.connect();
            Scanner scan = new Scanner(metadataraw);
            scan.useDelimiter("\\A");
            String strMetaData = scan.next();
            // System.out.println("Strmetadata: "+strMetaData);
            filesJson = gson.fromJson(strMetaData, FilesJson.class);
        } catch (NoSuchElementException ex) {
            File metadata = new File(this.chord.prefix + guid);       // Create file object with filepath
            metadata.createNewFile();                                         // Create the physical file

            // Create initial data for metadata
            filesJson = new FilesJson();
            filesJson.addFile(new FileJson("Metadata", Long.valueOf(0)));   // Add metadata entry

            // Write data to metadata file
            writeMetaData(filesJson);

        }
        return filesJson;
    }

    /**
     * writeMetaData write the metadata back to the chord
     */
    public void writeMetaData(FilesJson filesJson) throws Exception {
        long guid = md5("Metadata");
        ChordMessageInterface peer = chord.locateSuccessor(guid);

        Gson gson = new Gson();
        peer.put(guid, gson.toJson(filesJson));
    }

    /**
     * Changes the name of a file in the system
     *
     * @param oldName Name of file to be edited
     * @param newName New name of file
     * @throws Exception
     */
    public void move(String oldName, String newName) throws Exception {
        // Read Metadata
        FilesJson metadata = readMetaData();

        // Find and edit file
        for (int i = 0; i < metadata.file.size(); i++) {
            if (metadata.file.get(i).getName().equals(oldName)) {
                System.out.println("Move file found!\n");
                metadata.file.get(i).incrementRef();                    // Increment referenceCount
                writeMetaData(metadata);                                // Update metadata with new reference count
                metadata.file.get(i).name = newName;                    // Change old file name to newName
                metadata.file.get(i).writeTS = date.getTime();          // Update write timestamp
                metadata.file.get(i).decrementRef();                    // Decrement referenceCount
                writeMetaData(metadata);                                // Update metadata
                break;
            }
        }
    }


    /**
     * List the files in the system
     */
    public String lists() throws Exception {
        StringBuilder listOfFiles = new StringBuilder("\nFiles:\n");                        // Initialize string to hold file names

        List<FileJson> myFiles = readMetaData().file;               // Get our list of files
        for (int i = 0; i < myFiles.size(); i++) {
            listOfFiles.append(myFiles.get(i).name + "\n");              // Append each file name
        }
        listOfFiles.append("\n");
        return listOfFiles.toString();
    }

    /**
     * create an empty file
     *
     * @param fileName Name of the file
     */
    public void create(String fileName) throws Exception {
        // Create new file
        FileJson newFile = new FileJson(fileName, (long) 0);

        // Read Metadata
        FilesJson metadata = readMetaData();

        // Add new file to metadata
        metadata.file.add(newFile);

        // Write Metadata
        writeMetaData(metadata);
    }

    /**
     * delete file
     *
     * @param fileName Name of the file
     */
    public void delete(String fileName) throws Exception {
        // Read Metadata
        FilesJson metadata = readMetaData();

        // Find and delete file
        boolean find = false;
        for (int i = 0; i < metadata.file.size(); i++) {
            if (metadata.file.get(i).getName().equals(fileName)) {

                // Delete physical pages for file from chord
                for (int j = 0; j < metadata.file.get(i).pages.size(); j++) {
                    ChordMessageInterface peer = chord.locateSuccessor(metadata.file.get(i).pages.get(j).getGUID());        // Locate node where page is held
                    peer.delete(metadata.file.get(i).pages.get(j).getGUID());                       // Delete page
                }

                // Remove file from metadata
                metadata.file.remove(i);
                find = true;
            }
        }

        // Write Metadata if file was found, else return
        if (find) {
            writeMetaData(metadata);
        } else return;

    }

    /**
     * Read block pageNumber of fileName
     *
     * @param fileName   Name of the file
     * @param pageNumber number of block.
     */
    public RemoteInputFileStream read(String fileName, int pageNumber) throws Exception {
        // Read Metadata
        FilesJson metadata = readMetaData();

        // Find file
        boolean find = false;
        FileJson myFile = null;
        int fileIndex = 0;
        for (int i = 0; i < metadata.file.size(); i++) {
            if (metadata.file.get(i).getName().equals(fileName)) {
                fileIndex = i;
                metadata.file.get(i).incrementRef(pageNumber);                              // Increment file and page refCount
                myFile = metadata.file.get(i);
                metadata.file.get(i).readTS = date.getTime();                               // Update file read timestamp
                metadata.file.get(i).pages.get(pageNumber).readTS = date.getTime();         // Update page read timestamp
                writeMetaData(metadata);                                                    // Update metadata with refCount and timestamps
                find = true;
                break;
            }
        }

        // If file was found return page, else return null
        if (find) {
            PagesJson myPage = myFile.pages.get(pageNumber);
            metadata.file.get(fileIndex).decrementRef(pageNumber);                          // Decrement refCount
            writeMetaData(metadata);                                                        // Update metadata for read and refCount
            return chord.get(myPage.guid);
        } else return null;
    }

    /**
     * Read the first page for a file
     *
     * @param fileName Name of the file
     * @return First block of a file
     * @throws Exception
     */
    public RemoteInputFileStream head(String fileName) throws Exception {
        return read(fileName, 0);
    }

    /**
     * Read the last page for a file
     *
     * @param fileName Name of the file
     * @return Last block of a file
     * @throws Exception
     */
    public RemoteInputFileStream tail(String fileName) throws Exception {
        // Read Metadata
        FilesJson metadata = readMetaData();

        // Find file
        boolean find = false;
        FileJson myFile = null;
        int tailPage = 0;
        int fileIndex = 0;
        for (int i = 0; i < metadata.file.size(); i++) {
            if (metadata.file.get(i).getName().equals(fileName)) {
                fileIndex = i;
                myFile = metadata.file.get(i);
                tailPage = myFile.pages.size() - 1;
                metadata.file.get(i).readTS = date.getTime();                               // Update file read timestamp
                metadata.file.get(i).pages.get(tailPage).guid = date.getTime();             // Update page read timestamp
                metadata.file.get(i).incrementRef(tailPage);                                // Increment file and page referenceCount
                writeMetaData(metadata);                                                    // Update metadata with new RefCount and timestamps
                find = true;
                break;
            }
        }

        // If file was found return page, else return null
        if (find) {
            PagesJson myPage = myFile.pages.get(tailPage);
            metadata.file.get(fileIndex).decrementRef(tailPage);                            // Decrement reference count
            writeMetaData(metadata);                                                        // Update metadata for read and refCount
            return chord.get(myPage.guid);
        } else return null;
    }

    /**
     * Add a page to the file
     *
     * @param fileName Name of the file
     * @param data     RemoteInputStream.
     */
    public void append(String fileName, RemoteInputFileStream data) throws Exception {
        // Read Metadata
        FilesJson metadata = readMetaData();

        // Find file
        boolean find = false;
        int newPageIndex = 0;
        int fileIndex = 0;
        Long pageGUID = Long.valueOf(0);
        for (int i = 0; i < metadata.file.size(); i++) {
            if (metadata.file.get(i).getName().equals(fileName)) {
                fileIndex = i;
                metadata.file.get(i).incrementRef();                                            // Increment file refCount
                writeMetaData(metadata);                                                        // Write updated metadata
                newPageIndex = metadata.file.get(i).pages.size();
                pageGUID = md5(fileName + newPageIndex);
                metadata.file.get(i).addPage(new PagesJson(pageGUID, (long) data.total), (long) data.total);     // Add new page entry to file and update filesize
                metadata.file.get(i).writeTS = date.getTime();              // Update file write timestamp
                find = true;
                break;
            }
        }

        // If file was found append data and add to chord, else return
        if (find) {
            //Find closest successor node and place data
            metadata.file.get(fileIndex).decrementRef();                                    // Decrement refcount
            ChordMessageInterface peer = chord.locateSuccessor(pageGUID);
            writeMetaData(metadata);                                                        // Update metadata for write and refCount
            peer.put(pageGUID, data);
        } else return;
    }

}


