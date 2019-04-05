
import java.io.*;
import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DFSCommand
{
    DFS dfs;
        
    public DFSCommand(int p, int portToJoin) throws Exception {
        dfs = new DFS(p);
        
        if (portToJoin > 0)
        {
            System.out.println("Joining "+ portToJoin);
            dfs.join("127.0.0.1", portToJoin);
        }
        System.out.print("> ");
        BufferedReader buffer=new BufferedReader(new InputStreamReader(System.in));
        String line = buffer.readLine();  
        while (!line.equals("quit"))
        {
            String[] result = line.split("\\s");
            switch(result[0]) {
                case "join":
                    if (result.length == 2){
                        try {
                            dfs.join("127.0.0.1", Integer.parseInt(result[1]));
                        } catch (NumberFormatException e) {
                            System.out.println("Error - Second argument must be a port number.");
                        }
                    }
                    break;

                case "print":
                    dfs.print();
                    break;

                case "leave":
                    dfs.leave();
                    break;

                case "ls":
                    System.out.println(dfs.lists());
                    break;

                case "touch":
                    if (result.length == 2)
                        dfs.create(result[1]);                  // User must specify a fileList name
                    break;

                case "delete":
                    if (result.length == 2)
                        dfs.delete(result[1]);                  // User must specify fileList name
                    break;

                case "read":
                    if (result.length == 3) {
                        try {
                            var data = dfs.read(result[1], Integer.parseInt(result[2]));   // User must specify fileList name and page number
//                            data.connect();
//                            while (data.available() > 0) {
//                                System.out.print(data.readNBytes(20,0));
//                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Error - Second argument must be a page number.");
                        }
                    }
                    break;

                case "tail":
                    if (result.length == 2) {
                        dfs.tail(result[1]);                    // User must specify fileList name
                    }
                    break;

                case "head":
                    if (result.length == 2) {
                        dfs.head(result[1]);                    // User must specify fileList name
                    }
                    break;

                case "append":
                    if (result.length == 3) {
                        // Append text by enclosing in single quotes [']
                        if (result[2].matches("^'(.+?)'$")) {
                            Pattern pattern = Pattern.compile("^'(.+?)'$");
                            Matcher matcher = pattern.matcher(result[2]);
                            matcher.find();
                            String text = matcher.group(1);

                            dfs.append(result[1], text); // Appends text
                        }
                        else
                            dfs.append(result[1], new RemoteInputFileStream(result[2]));        // User must specify filename they want to append data to and filepath of the data to be appended
                    }
                    break;

                case "move":
                    if (result.length == 3) {
                        dfs.move(result[1], result[2]);         // User must specify fileList to be edited and its new name
                    }
                    break;

                default:
                    System.out.println("Error - Not a valid command.");
                    break;
            }

//            if (result[0].equals("join")  && result.length > 1)
//            {
//                dfs.join("127.0.0.1", Integer.parseInt(result[1]));
//            }
//            if (result[0].equals("print"))
//            {
//                dfs.print();
//            }
//            if (result[0].equals("leave"))
//            {
//                dfs.leave();
//            }

            // User interface:
            // join, ls, touch, delete, read, tail, head, append, move
//            if (result[0].equals("ls"))
//            {
//                System.out.println(dfs.lists());
//            }
//            if (result[0].equals("touch"))
//            {
//                dfs.create(result[1]);                  // User must specify fileList name
//            }
//            if (result[0].equals("delete"))
//            {
//                dfs.delete(result[1]);                  // User must specify fileList name
//            }
//            if (result[0].equals("read"))
//            {
//                dfs.read(result[1], Integer.parseInt(result[2]));   // User must specify fileList name and page number
//            }
//            if (result[0].equals("tail"))
//            {
//                dfs.tail(result[1]);                    // User must specify fileList name
//            }
//            if (result[0].equals("head"))
//            {
//                dfs.head(result[1]);                    // User must specify fileList name
//            }
//            if (result[0].equals("append"))
//            {
//                dfs.append(result[1], new RemoteInputFileStream(result[2]));        // User must specify filename they want to append data to and filepath of the data to be appended
//            }
//            if (result[0].equals("move"))
//            {
//                dfs.move(result[1], result[2]);         // User must specify fileList to be edited and its new name
//            }
            System.out.print("> ");
            line=buffer.readLine();  
        }
        // If user inputs quit, exit program
        System.exit(0);
    }
    
    static public void main(String args[]) throws Exception
    {
        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));

        DFSCommand dfsCommand=new DFSCommand(2000, 2001);

//        // String to be scanned to find the pattern.
//        String line = "This order was placed for QT3000! OK?";
//        String pattern = "(.*)(\\d+)(.*)";
//
//        // Create a Pattern object
//        Pattern r = Pattern.compile(pattern);
//
//        // Now create matcher object.
//        Matcher m = r.matcher(line);
//
//        if (m.find( )) {
//            System.out.println("Found value: " + m.group(0) );
//            System.out.println("Found value: " + m.group(1) );
//            System.out.println("Found value: " + m.group(2) );
//        } else {
//            System.out.println("NO MATCH");
//        }


//        if (args.length < 1 ) {
//            throw new IllegalArgumentException("Parameter: <port> <portToJoin>");
//        }
//        if (args.length > 1 ) {
//            DFSCommand dfsCommand=new DFSCommand(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
//        }
//        else
//        {
//            DFSCommand dfsCommand=new DFSCommand( Integer.parseInt(args[0]), 0);
//        }
     } 
}
