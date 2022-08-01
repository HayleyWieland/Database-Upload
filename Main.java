import java.io.*;
import java.sql.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;


/**
 * This program connects to the local curriculum database and loads it using CSV files 
 * In the first section, the program reads through the directory that houses the lesson plans for the database & stores their file paths, then it 
 * stores the filepaths in order in a CSV file so that it can be uploaded into the lessons table
 */
public class Main {

    public static void main (String[] args) throws Exception{


        //this section gets the file paths for the lesson plans and then saves them in order to a csv file
        try {
            String filepath = "/Users/teacher/Desktop/Lesson Plans/";

            File lessonPlanDirectory = new File(filepath);

            //create an array of files
            File[] files = lessonPlanDirectory.listFiles();
            //create a blank string to hold the names of the files
            String filenames = "";

            /*pass the files, filepath, and blank string into a method that gets all the file paths of the files in every
            subdirectory and returns them in a csv formatted string */
            filenames = listLessonPlanExt(files, filepath, filenames);

            //split the string into an array of file path names
            String [] fileNamesArray = filenames.split(",");

            /*sort the file path names so that they are in order - each lesson plan file has it's lesson plan id number
             on it as the last number in parentheses
             */
            Arrays.sort(fileNamesArray, new Comparator<String>() {
                @Override
                    public int compare(String str1, String str2){
                    //get last index of "(" and last index of ")" for each string
                    int ssStart1 = str1.lastIndexOf("(");
                    int ssEnd1 = str1.lastIndexOf(")");

                    int ssStart2 = str2.lastIndexOf("(");
                    int ssEnd2 = str2.lastIndexOf(")");
                    //then use substring to get the number in between the last set of parentheses
                        String substr1 = str1.substring(ssStart1 +1, ssEnd1);
                        String substr2 = str2.substring(ssStart2 +1, ssEnd2);

                        //sort the strings from lowest to highest
                        return Integer.valueOf(substr1).compareTo(Integer.valueOf(substr2));
                    }

            });

            //create the csv file to hold the file paths
            File csvLessonPlanPaths = new File("./LessonPlanPaths.csv");

            //create the PrintWriter to write to the csv file
            PrintWriter pw = new PrintWriter(csvLessonPlanPaths);

            //loop through the array and write each file path in csv format
            for(String fileName: fileNamesArray){
                pw.println(fileName + ",");
            }

            pw.close();

        }
        catch (Exception e){
            System.out.println("An error has occurred.");
            e.printStackTrace();
        }


        //connect to the database
        try
        {
            //get database user and password from user
            Scanner keyboard = new Scanner(System.in);
            System.out.println("Enter the database user: ");
            String user = keyboard.nextLine();

            System.out.println("Enter the database password: ");
            String password = keyboard.nextLine();
            //connect to the database
            Connection conn = DriverManager.getConnection("jdbc:mariadb://localhost:3306/curriculum", user, password);
            System.out.println("connection established");

            //create a Statement object for the connection
            Statement st = conn.createStatement();

            //run the method that loads the category table
            loadCategoryTable(st);

            //run the method that loads the Song table
            loadSongTable(st);

            //run the method that loads the belongs_to table
            loadBelongsToTable(st);

            //run the method that loads the materials table
            loadMaterialsTable(st);

            //run the method that loads the lessons table
            loadLessonsTable(st);

            //run the method that loads the uses table
            loadUsesTable(st);

            //run the method that loads the standards table
            loadStandardsTable(st);

            //run the method that loads the meets table
            loadMeetsTable(st);

            //run the method that loads the concepts table
            loadConceptsTable(st);

            //run the method that loads the teaches table
            loadTeachesTable(st);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        }
    
    public static String listLessonPlanExt(File [] files, String filepath, String filenames){
        try {
            String parentDirectory = filepath;
            for (File file : files) {
                //if a file is found, write the pathname to the csv file
                if (file.isFile()) {
                    //only write the file if it's not a .ds file or duplicate
                    if(!file.getName().contains(".DS_Store") && !file.getName().contains("~$")) {

                        filenames += file + ",";
                    }

                }
                //if a directory is found, open the directory
               if (file.isDirectory()){
                    //make the directory part of its parent directory
                    filepath = parentDirectory;
                    //add the name of the current directory to the path 
                    filepath += file.getName() + "/";
                    //list the files in the current directory
                    files = file.listFiles();
                    //repeat to find any subdirectories & save the names of any files found to the filenames string
                    filenames = listLessonPlanExt(files, filepath, filenames);
                }
                filepath = parentDirectory; //return to the parent directory when done

            }
            return filenames; 

        }
        catch (Exception e){
            System.out.println("An error has occurred with reading through the directory.");
            e.printStackTrace();
        }

        return filenames;
    }

    public static void loadCategoryTable(Statement statement) throws FileNotFoundException, SQLException {
        //get the category csv file and create a file reader for it
        File csvFile = new File("/Users/teacher/Desktop/Tables/Tables - CATEGORY.csv");
        Scanner scanner = new Scanner(csvFile);

        //skip over the column titles 
        scanner.nextLine();


        //loop through the csv file, creating an insert statement for each entry
        while(scanner.hasNext()){
            //get a line for the entry
            String entry = scanner.nextLine();
            //split that line into the two parts of the entry
            String [] fields = entry.split(",");
            //first entry is the category code (type =  int)
            String categoryCode = fields[0];
            //second entry is the category title (type = varchar)
            String categoryTitle = "'" + fields[1] +"'";
            //create the insert statement
            String insertString = String.format("INSERT INTO category(category_code, category_title) values(%s, %s)", categoryCode, categoryTitle);
            //run the insert statement
            try {
                statement.executeUpdate(insertString);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }


    }

    public static void loadSongTable(Statement statement) throws FileNotFoundException {
        //get the category csv file and create a file reader for it
        File csvFile = new File("/Users/teacher/Desktop/Tables/Tables - SONG.csv");
        Scanner scanner = new Scanner(csvFile);

        //skip over the column titles 
        scanner.nextLine();

            //loop through the csv file, creating an insert statement for each entry
            while (scanner.hasNext()) {
                //get a line for the entry
                String entry = scanner.nextLine();
                //split that line into the two parts of the entry
                String[] fields = new String[8];
                fields = entry.split(",");
                //first entry is the song code (type = int)
                String songCode = fields[0];
                //second entry is the title (type = varchar)
                String songTitle = fields[1];
                //search for any apostrophes in the song title
                if(songTitle.contains("'")){
                  //replace any apostrophes with an escape sequence
                    songTitle = songTitle.replaceAll("'", "\\\\'");
                }
                songTitle = "'" + songTitle + "'";
                //third entry is the composer (type = varchar)
                String composer = fields[2];
                //search for any apostrophes in the composer's name
                if(composer.contains("'")){
                    //replace any apostrophes with an escape sequence
                    composer = composer.replaceAll("'", "\\\\'");
                }
                composer = "'" + composer + "'";
                //fourth entry is the notes (type = varchar)
                String notes =  fields[3];
                //search for any apostrophes in the notes
                if(notes.contains("'")){
                    //replace any apostrophes with an escape sequence
                    notes = notes.replaceAll("'", "\\\\'");
                }
                notes = "'" + notes + "'";
                //fifth entry is the source (type = varchar)
                String source = "'" + fields[4] + "'";
                //sixth entry is the source2 (type = varchar)
                String source2 = "'" + fields[5] + "'";
                //seventh entry is the language (type = varchar)
                String language = "'" + fields[6] + "'";
                //eighth entry is the origin (type = varchar)
                String origin = "'" + fields[7] + "'";
                //ninth entry is the link (type = varchar)
                String link = "'" + "'";
                if(fields.length == 9) {
                    link = "'" + fields[8] + "'";
                }

                //create the insert statement
                String insertString = String.format("INSERT INTO song(song_code, title, composer, notes, source, source_2, language, origin, link) " +
                                "values(%s, %s, %s, %s, %s, %s, %s, %s, %s)",
                        songCode, songTitle, composer, notes, source, source2, language, origin, link);

                //run the insert statement
                try {
                    statement.executeUpdate(insertString);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

            }
        }

    public static void loadBelongsToTable(Statement statement) throws FileNotFoundException {
        //get the category csv file and create a file reader for it
        File csvFile = new File("/Users/teacher/Desktop/Tables/Tables - BELONGS_TO.csv");
        Scanner scanner = new Scanner(csvFile);

        //skip over the column titles 
        scanner.nextLine();

        //loop through the csv file, creating an insert statement for each entry
        while (scanner.hasNext()) {
            //get the entry
            String entry = scanner.nextLine();

            //split the entry into an array of the attributes
            String [] fields = entry.split(",");
            //the first entry item is the category_code (type = int)
            String categoryCode = fields[0];

            //the second entry item is the song_code (type = int)
            String songCode = fields[1];
            
            //create the insert statement 
            String insertString = String.format("INSERT INTO belongs_to (category_code, song_code) values ( %s, %s)", categoryCode, songCode);

            //run the insert statement
            try {
                statement.executeUpdate(insertString);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }


    }

    public static void loadMaterialsTable(Statement statement) throws FileNotFoundException {
        //get the category csv file and create a file reader for it
        File csvFile = new File("/Users/teacher/Desktop/Tables/Tables - MATERIALS.csv");
        Scanner scanner = new Scanner(csvFile);

        //skip over the column titles 
        scanner.nextLine();

        //loop through the csv file, creating an insert statement for each entry
        while (scanner.hasNext()) {
            //get the entry
            String entry = scanner.nextLine();

            //split the entry into an array of the attributes
            String [] fields = entry.split(",");
            //the first entry item is the material_code (type = int)
            String materialCode = fields[0];

            //the second entry item is the material_name (type = varchar)
            String materialName = "'" + fields[1] + "'";
            
            //create the insert statement
            String insertString = String.format("INSERT INTO materials(material_code, material_name) values ( %s, %s)", materialCode, materialName);

            //run the insert statement
            try {
                statement.executeUpdate(insertString);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void loadLessonsTable(Statement statement) throws FileNotFoundException {
        //get the file & pass it to a scanner
        File csvFile = new File("/Users/teacher/Desktop/Tables/Tables - LESSONS.csv");
        Scanner scanner = new Scanner(csvFile);

        //get the lesson plan paths & pass it to a scanner
        File csvLessonPlanPaths = new File("./LessonPlanPaths.csv");
        Scanner pathScanner = new Scanner(csvLessonPlanPaths);

        //skip over the column titles
        scanner.nextLine();

        //loop through the entries & create an insert statement to import into the database
        while (scanner.hasNext()){
            //get the entry
            String entry = scanner.nextLine();
            //split the entry into fields
            String [] fields = entry.split(",");

            //first entry is LESSON_PLAN_ID (type = int)
            String lessonPlanID = fields[0];

            //second entry is SONG_CODE (type = int)
            String songCode = fields[1];

            //third entry is LESSON_NUMBER (type = varchar)
            String lessonNumber = "'" + fields[2] + "'";

            //fourth entry is GRADE_LEVEL (type = varchar)
            String gradeLevel = "'" + fields[3] +"'";

            //fifth entry is the lesson plan path (LESSON_PLAN), get from csv file from earlier (type = varchar)
            String lessonPlan =  pathScanner.nextLine();
            //get rid of the comma
            lessonPlan = "'" + lessonPlan.substring(0, lessonPlan.length()-1) + "'";

            //create insert statement
            String insertString = String.format("INSERT INTO Lessons(lesson_plan_id, song_code, lesson_number, grade_level, Lesson_plan_path)" +
                    " values (%s, %s, %s, %s, %s)", lessonPlanID, songCode, lessonNumber, gradeLevel, lessonPlan);

            //run the insert statement         
            try{
                statement.executeUpdate(insertString);
            }catch (SQLException e){
                throw new RuntimeException(e);
            }
        }
    }

    public static void loadUsesTable(Statement statement) throws FileNotFoundException {
        //get the file & pass it to a scanner
        File csvFile = new File("/Users/teacher/Desktop/Tables/Tables - USES.csv");
        Scanner scanner = new Scanner(csvFile);

        //skip over the column titles
        scanner.nextLine();

        //loop through the entries & create an insert statement to import into the database
        while (scanner.hasNext()) {
            //get the entry
            String entry = scanner.nextLine();
            //split the entry into fields
            String[] fields = entry.split(",");

            //first field is LESSON_PLAN_ID (type = int)
            String lessonPlanID = fields[0];

            //second field is MATERIAL_CODE (type = int)
            String materialCode = fields[1];

            //create insert statement
            String insertString = String.format("INSERT INTO uses(Lesson_plan_id, material_code) " +
                    "values( %s, %s)", lessonPlanID, materialCode);
            //run the insert statement
            try {
                statement.executeUpdate(insertString);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static void loadStandardsTable(Statement statement) throws FileNotFoundException{
        //get the file & pass it to a scanner
        File csvFile = new File("/Users/teacher/Desktop/Tables/Tables - STANDARDS.csv");
        Scanner scanner = new Scanner(csvFile);

        //skip over the column titles
        scanner.nextLine();

        //loop through the entries & create an insert statement to import into the database
        while (scanner.hasNext()) {
            //get the entry
            String entry = scanner.nextLine();
            //split the entry into fields
            String[] fields = entry.split(",");

            //first field is Standard_Key (type = int)
            String standardKey = fields[0];

            //second field is Standard_Type (type = varchar)
            String standardType = "'" + fields[1] + "'";

            //third field is Standard_Code (type = varchar)
            String standardCode = "'" + fields[2] + "'";

            //fourth field is Standard_Description (type = varchar)
            String standardDescription = fields[3];

            //loop through the rest of the array to add it to the description string to account for any commas
            if(fields.length > 4){
                //start where the array left off
                int i = 4;
                //loop through the rest of the array, adding the comma back in & adding in the sentence chunk
                while(i < fields.length){
                    standardDescription = standardDescription + "," + fields[i];
                    i++;
                }
            }

            standardDescription = "'" + standardDescription + "'";

            //create insert statement
            String insertString = String.format("INSERT INTO standards(Standard_Key, Standard_Type, Standard_Code, Standard_Description)" +
                    "values(%s, %s, %s, %s)", standardKey, standardType, standardCode, standardDescription);

            //run insert statement
            try{
                statement.executeUpdate(insertString);
            }
            catch (SQLException e){
                throw new RuntimeException(e);
            }

        }
    }

    public static void loadMeetsTable(Statement statement) throws FileNotFoundException{
        //get the file and pass it to the scanner
        File csvFile = new File("/Users/teacher/Desktop/Tables/Tables - MEETS.csv");
        Scanner scanner = new Scanner(csvFile);

        //skip over the column names
        scanner.nextLine();

        //read through each line of the file & enter it into the database
        while(scanner.hasNext()){
            //get the entry line
            String entry = scanner.nextLine();
            //split it into fields
            String [] fields = entry.split(",");

            //first field is lesson_plan_id (type = int)
            String lessonPlanID = fields[0];

            //second field is standard_key (type = int)
            String standardKey = fields[1];

            //create insert statement
            String insertString = String.format("INSERT INTO meets(lesson_plan_id, standard_key) " +
                    " values(%s, %s)", lessonPlanID, standardKey);

            //run the insert statement
            try{
                statement.executeUpdate(insertString);
            }catch (SQLException e){
                throw new RuntimeException(e);
            }
        }
    }

    public static void loadConceptsTable(Statement statement) throws FileNotFoundException{
        //get file & pass it to a Scanner
        File csvFile = new File("/Users/teacher/Desktop/Tables/Tables - CONCEPTS.csv");
        Scanner scanner = new Scanner(csvFile);

        //skip over the column headers
        scanner.nextLine();

        while (scanner.hasNext()){
            //get the entry
            String entry = scanner.nextLine();
            //split the entry into it's fields
            String [] fields = entry.split(",");

            //first field is concept_code (type = int)
            String conceptCode = fields[0];

            //second field is concept_name (type = varchar)
            String conceptName = "'" + fields[1] + "'";

            //third field is concept_description (type = varchar)
            String conceptDescription = "'" fields[2] + "'";

            //create the insert statement
            String insertStatement = String.format("INSERT INTO concepts(concept_code, concept_name, concept_description) " +
                    "values(%s, %s)", conceptCode, conceptName, conceptDescription);

            try{
                statement.executeUpdate(insertStatement);
            }
            catch (SQLException e){
                throw new RuntimeException(e);
            }

        }
    }

    public static void loadTeachesTable(Statement statement) throws FileNotFoundException{
        //get the file & pass it to a Scanner
        File csvFile = new File("/Users/teacher/Desktop/Tables/Tables - TEACHES.csv");
        Scanner scanner = new Scanner(csvFile);

        //skip the column headers
        scanner.nextLine();

        //loop through the file, creating an entry in the database for each line
        while (scanner.hasNext()){
            //get the entry
            String entry = scanner.nextLine();
            //split the entry into it's fields
            String [] fields = entry.split(",");

            //first field is lesson_plan_id (type = int)
            String lessonPlanID = fields[0];

            //second field is concept_code (type = int)
            String concept_code = fields[1];

            //create insert statement
            String insertString = String.format("INSERT INTO teaches(lesson_plan_id, concept_code)" +
                    "values(%s, %S)", lessonPlanID, concept_code);
            try{
                statement.executeUpdate(insertString);
            }
            catch (SQLException e){
                throw new RuntimeException(e);
            }
        }
    }
        }





