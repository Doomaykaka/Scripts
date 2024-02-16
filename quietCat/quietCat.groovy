import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import groovy.cli.commons.CliBuilder
import java.nio.file.Path
import java.nio.file.Paths
import groovy.io.FileType
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.json.*

class Constants {

    def static final EXCLUSION_FILENAMES = ['quietCat.groovy', 'Templates']

}

class Template {

    Insertations insertations
    List<TemplateFolder> templateFolders
    List<TemplateFile> templateFiles
    String name
    String description
    Path currentPath = Paths.get("./")

    Template (String name, String description) {

        this.insertations = new Insertations()
        this.templateFolders = []
        this.templateFiles = []
        this.name = name
        this.description = description

    }

    def parse() {

        File currentFolder = new File(currentPath.toString())

        for (File folderElement : currentFolder.listFiles()) {
            if (!Constants.EXCLUSION_FILENAMES.contains(folderElement.getName())) {
                insertations.parse(folderElement.getName())

                if (folderElement.isDirectory()) {
                    TemplateFolder folder = new TemplateFolder(currentPath, folderElement.getName())
                    folder.parse(insertations)
                    templateFolders.add(folder)
                } else {
                    TemplateFile file = new TemplateFile(currentPath, folderElement.getName())
                    file.parse(insertations)
                    templateFiles.add(file)
                }
            }
        }

    }

    def generate(){

        for (TemplateFolder folder : templateFolders) {
            folder.generate(insertations)
        }

        for (TemplateFile file : templateFiles) {
            file.generate(insertations)
        }
    }

    def save(Path pathToTemplateFile){
        def insertationsData = [:]

        for (entry in (this.insertations).getInsertationsMap()) {
            insertationsData.put(
                                    [
                                        "insertation":
                                            [
                                                "definition": entry.key.getDefinition(),
                                                "type": entry.key.getType(),
                                                "description": entry.key.getDescription(),
                                                "defaultValue": entry.key.getDefaultValue(),
                                            ]
                                    ]
                                    ,
                                    [
                                        "value":entry.value
                                    ]

                                )
        }

        def templateFoldersData = []

        for (templateFolder in this.templateFolders){
            templateFoldersData.add(templateFolder.save())
        }

        def templateFilesData = []

        for (templateFile in this.templateFiles){
            templateFilesData.add(templateFile.save())
        }

        def templateData = [
                                "insertations": insertationsData,
                                "templateFolders": templateFoldersData,
                                "templateFiles": templateFilesData,
                                "name": this.name,
                                "description": this.description,
                            ]

        def json = new JsonBuilder(templateData).toPrettyString()

        File saveFile = new File(pathToTemplateFile.toString())

        FileWriter fw = new FileWriter(saveFile)
        fw.write(json)
        fw.close()
    }

    def load(Path pathToTemplateFile){
        def jsonSlurper = new JsonSlurper()

        def json = ""

        File saveFile = new File(pathToTemplateFile.toString())

        BufferedReader reader = new BufferedReader(new FileReader(saveFile))
        String line;
        while ((line = reader.readLine()) != null) {
            json += line
        }
        reader.close();

        def dataSet = jsonSlurper.parseText(json)

        def name = dataSet.name
        def description = dataSet.description
        this.name = name
        this.description = description

        def insertionsData = dataSet.insertations

        Insertations newInsertations = new Insertations()

        for (entry : insertionsData) {
            def key = entry.key
            def value = entry.value.value

            String definition = (key =~ /\{insertation=\{definition=(.+), type=/)[0][1]

            Insertation newInsertation = new Insertation(definition)

            newInsertations.getInsertationsMap().put(newInsertation, value)
        }

        this.insertations = newInsertations

        def templateFoldersData = dataSet.templateFolders
        def templateFilesData = dataSet.templateFiles

        for(templateFoldersGroup : templateFoldersData) {
            for (templateFolder : templateFoldersGroup) {
                TemplateFolder newTemplateFolder = new TemplateFolder(Paths.get("."), "default")

                newTemplateFolder.load(templateFolder)

                this.templateFolders.add(newTemplateFolder)
            }
        }

        for(templateFileGroup : templateFilesData) {
            for (templateFile : templateFileGroup.templateFile) {
                TemplateFile newTemplateFile = new TemplateFile(Paths.get("."), "default")
                newTemplateFile.load(templateFile)

                this.templateFiles.add(newTemplateFile)
            }
        }
    }

}

class TemplateFolder{

    List<TemplateFolder> templateFolders
    List<TemplateFile> templateFiles
    String nameWithInsertation
    Path currentPath

    def TemplateFolder(Path parentDir, String nameWithInsertation){
        templateFolders = []
        templateFiles = []
        this.nameWithInsertation = nameWithInsertation
        currentPath = Paths.get(parentDir.toString(), nameWithInsertation)
    }

    void parse(Insertations insertations){
        File currentFolder = new File(currentPath.toString())

        for(File folderElement : currentFolder.listFiles()){
            if(!Constants.EXCLUSION_FILENAMES.contains(folderElement.getName())){
                insertations.parse(folderElement.getName())

                if(folderElement.isDirectory()){
                    TemplateFolder folder = new TemplateFolder(currentPath, folderElement.getName())
                    folder.parse(insertations)
                    templateFolders.add(folder)
                } else {
                    TemplateFile file = new TemplateFile(currentPath, folderElement.getName())
                    file.parse(insertations)
                    templateFiles.add(file)
                }
            }
        }
    }

    void generate(Insertations insertations) {
        File folder
        Path pathToFile

        if(insertations.elementsCount() != 0){
            pathToFile = Paths.get(currentPath.parent.toString(), insertations.replace(nameWithInsertation.toString()))
        } else {
            pathToFile = Paths.get(currentPath.parent.toString(), nameWithInsertation.toString())
        }

        folder = new File(pathToFile.toString())

        folder.mkdirs()

        for (TemplateFolder childFolder : templateFolders) {
            childFolder.generate(insertations)
        }

        for (TemplateFile childFile : templateFiles) {
            childFile.generate(insertations)
        }
    }

    List save() {
        def saveStruct = []

        def templateFoldersStruct = []

        for (raw : templateFolders) {
            templateFoldersStruct.add(raw.save())
        }

        def templateFilesStruct = []

        for (raw : templateFiles) {
            templateFilesStruct.add(raw.save())
        }

        saveStruct.add(
            "templateFolder":
            [
                'templateFolders' : templateFoldersStruct,
                'templateFiles' : templateFilesStruct,
                'nameWithInsertation' : nameWithInsertation,
                'currentPath' : currentPath.toString(),
            ]
        )

        return saveStruct
    }

    def load(def templateFolderData) {
        if (templateFolderData != null) {
            def templateFoldersData = templateFolderData.templateFolder.templateFolders
            def templateFilesData = templateFolderData.templateFolder.templateFiles


            for(templateFoldersGroup : templateFoldersData) {
                for (templateFolder : templateFoldersGroup) {
                    if(templateFolder != null){
                        TemplateFolder newTemplateFolder = new TemplateFolder(Paths.get("."), "default")
                        newTemplateFolder.load(templateFolder)

                        this.templateFolders.add(newTemplateFolder)
                    }
                }
            }

            for(templateFilesGroup : templateFilesData) {
                for (templateFile : templateFilesGroup) {
                    TemplateFile newTemplateFile = new TemplateFile(Paths.get("."), "default")
                    newTemplateFile.load(templateFile.templateFile)

                    this.templateFiles.add(newTemplateFile)
                }
            }

            def nameWithInsertationData = templateFolderData.templateFolder.nameWithInsertation

            def currentPathData = templateFolderData.templateFolder.currentPath

            this.nameWithInsertation = nameWithInsertationData
            this.currentPath = Paths.get(currentPathData.toString())
        }
    }

}

class TemplateFile {

    List<String> content
    String nameWithInsertation
    Path currentPath

    TemplateFile(Path parentDir, String nameWithInsertation) {
        content = []
        this.nameWithInsertation = nameWithInsertation
        currentPath = Paths.get(parentDir.toString(), nameWithInsertation)
    }

    void parse(Insertations insertations) {
        File currentFile = new File(currentPath.toString())

        if (currentFile.exists()) {
            Scanner scn = new Scanner(currentFile)
            while (scn.hasNextLine()) {
                def chunk = scn.nextLine()

                insertations.parse(chunk)
                content.add(chunk)
            }
            scn.close()
        }
    }

    void generate(Insertations insertations) {
        File file

        if(insertations.elementsCount() != 0){
            file = new File(Paths.get(insertations.replace(currentPath.parent.toString()), insertations.replace(nameWithInsertation)).toString())
        } else {
            file = new File(Paths.get(currentPath.parent.toString(), nameWithInsertation).toString())
        }

        file.createNewFile()

        FileWriter fw = new FileWriter(file)

        for(String raw : content){
            if(insertations.elementsCount() != 0){
                fw.write(insertations.replace(raw) + "\n")
            } else {
                fw.write(raw + "\n")
            }
        }

        fw.close()

    }

    List save() {
        def saveStruct = []

        def contentStruct = []

        for (raw : content) {
            contentStruct.add(raw)
        }

        saveStruct.add("templateFile":
                        [
                            'content' : contentStruct,
                            'nameWithInsertation' : nameWithInsertation,
                            'currentPath' : currentPath.toString(),
                        ]
        )

        return saveStruct
    }

    def load(def templateFileData) {
        if(templateFileData != null){
            def contentData = templateFileData.content

            for (raw : contentData) {
                content.add(raw)
            }

            def nameWithInsertationData = templateFileData.nameWithInsertation
            def currentPathData = templateFileData.currentPath

            this.nameWithInsertation = nameWithInsertationData
            this.currentPath = Paths.get(currentPathData.toString())
        }
    }

}

class Insertations {

    Map<Insertation, String> insertationsMap

    Insertations() {
        insertationsMap = [:]
    }

    Boolean put(Insertation insertation, String value) {
        if (insertationsMap.containsKey(insertation)) {
            if (validateInsertationValue(insertation, value)) {
                insertationsMap.put(insertation, value)

                return true
            }
        }

        return false
    }

    void parse(String text){
        def matches = text =~ /\[\-.*\-\]/

        while(matches.find()){
            def match = matches.group()

            Insertation insertation = new Insertation(match)
            insertationsMap.put(insertation, "")
        }
    }

    String replace(String text){
        String result = text

        for (Insertation insertation:insertationsMap.keySet()) {
            if (insertationsMap.get(insertation) == "") {
                result = result.replace(insertation.definition , insertation.defaultValue)
            } else {
                result = result.replace(insertation.definition, insertationsMap.get(insertation))
            }
        }

        return result
    }

    void clear() {
        insertationsMap.clear()
    }

    Integer elementsCount() {
        return insertationsMap.size()
    }

    private Boolean validateInsertationValue(Insertation insertation, String value) {
        String typeRegexp = insertation.type

        return value.matches(typeRegexp)
    }
}

class Insertation {

    String definition
    String type
    String description
    String defaultValue

    Insertation(String definition){
        this.definition = definition
        this.type = "*"
        this.description = "default"
        this.defaultValue = null

        parseInsertation()
    }

    def parseInsertation() {
        def insertationParts = definition[2..-3].split(";")
        this.type = (insertationParts[0]).replaceFirst("type=", "")
        this.description = (insertationParts[1]).replaceFirst("description=", "")
        this.defaultValue = (insertationParts[2]).replaceFirst("default=", "")

        if(this.type == "ALL"){
            this.type = ".*"
        }
    }
}

def app() {
    printAppOutput(text="Hello", is_title=false, level=0, prefix="", postfix="", needNewLine=true)
    printAppOutput(text="QuietCat", is_title=true, level=0, prefix="", postfix="", needNewLine=true)

    createCLI()

    printAppOutput(text="Done!", is_title=false, level=0, prefix="", postfix="", needNewLine=true)
}

def printAppOutput(String text, Boolean is_title, Integer level, String prefix, String postfix, Boolean needNewLine) {
    outputText = text

    if(is_title){
        outputText = "//----------" + text + "----------\\\\"
        if(needNewLine){
            println(outputText)
        } else {
            print(outputText)
        }

        return
    }

    if(prefix) {
        outputText = prefix + outputText
    }

    if(postfix) {
        outputText = outputText + postfix
    }

    if(level > 0) {
        outputText = " " * level + outputText
    }

    if(needNewLine){
        println(outputText)
    } else {
        print(outputText)
    }
}

def createCLI(){
    cliAnswer = createCLIQuestion(
                    questionText = "You need create or load template? (0 - create, 1 - load)",
                    isAppQuestion = true,
                )

    if (cliAnswer.toString().isInteger()) {
        cliAnswer = cliAnswer.toString() as Integer
    } else {
        cliAnswer = -1

        printAppOutput(text="Bad answer", is_title=false, level=0, prefix="!", postfix="", needNewLine=true)
        createCLI()
        return
    }

    templatesPath = downloadTemplates()

    switch (cliAnswer) {
        case 0:
            createTemplateCLI(templatesPath)
            break
        case 1:
            loadTemplateCLI(templatesPath)
            break
        default:
            printAppOutput(text="Bad answer", is_title=false, level=0, prefix="!", postfix="", needNewLine=true)
            createCLI()
            return
    }

    printAppOutput(text="Clearing", is_title=false, level=0, prefix="", postfix="", needNewLine=true)

    clearTemplatesFolder()
}

def Path downloadTemplates(){
    gitRepo = createCLIQuestion(questionText = "Input git repo with templates link (url)", isAppQuestion = true)

    executeCommands(
        [
            "mkdir",
            "Templates",
        ].join(" "),
        [
            "cd",
            "Templates",
        ].join(" "),
        [
            "git",
            "clone",
            gitRepo,
        ].join(" "),
    )

    repoFolderNameMatches = (gitRepo =~ /\/(.+)\.git/)
    repoFolderNameFirstMatch = repoFolderNameMatches[0]
    repoFolderNameFirstFullMatch = repoFolderNameFirstMatch[1]

    Path pathToTemplates = Paths.get(".", "Templates", repoFolderNameFirstFullMatch)

    return pathToTemplates
}

def createTemplateCLI(Path pathToTemplatesFolder){
    templateName = createCLIQuestion(
                                        questionText = "Input template name (example: SimpleApplication)",
                                        isAppQuestion = false
                                    )
    templateDescription = createCLIQuestion(
                                                questionText =
                                                "Input template description (example: simple application)",
                                                isAppQuestion = false
                                            )

    Template newTemplate = new Template(name = templateName, description = templateDescription)
    newTemplate.parse()

    newTemplate.save(Paths.get(pathToTemplatesFolder.toString(), templateName + ".json"))
    uploadTemplates(pathToTemplatesFolder.toString())
}

def uploadTemplates(String pathToTemplatesFolder) {
    executeCommands(
        [
            "cd",
            """\"${pathToTemplatesFolder}\"""",
        ].join(" "),
        [
            "git",
            "add",
            ".",
        ].join(" "),
        [
            "git",
            "commit",
            "-m",
            "\"Update\"",
        ].join(" "),
        [
            "git",
            "push",
        ].join(" "),
    )
}

def loadTemplateCLI(Path pathToTemplatesFolder) {
    templatesList = loadTemplatesList(pathToTemplatesFolder)

    chosenTemplateId = chooseTemplateCLI(templatesList)

    fullTemplateName = templatesList.get(chosenTemplateId)

    Template loadedTemplate = new Template(name = fullTemplateName, description = "default")
    loadedTemplate.load(Paths.get(pathToTemplatesFolder.toString(), fullTemplateName))

    insertValues(loadedTemplate)

    loadedTemplate.generate()
}

Integer chooseTemplateCLI(Map templatesList){
    printAppOutput(text = "Choose template (example: 1...)", is_title=false, level=2, prefix="[", postfix="]?: ", needNewLine=true)

    for(entry : templatesList){
        printAppOutput(text = """(${entry.key}) - (${entry.value})""", is_title=false, level=3, prefix="", postfix="", needNewLine=true)
    }

    templateId = createCLIQuestion(questionText = "Choose", isAppQuestion = true)

    if (templateId.toString().isInteger()) {
        templateId = templateId.toString() as Integer
    } else {
        templateId = -1

        printAppOutput(text="Bad answer", is_title=false, level=0, prefix="!", postfix="", needNewLine=true)
        chooseTemplateCLI(templatesList)
        return
    }

    if(!templatesList.containsKey(templateId)){
        printAppOutput(text="Bad answer", is_title=false, level=0, prefix="!", postfix="", needNewLine=true)
        chooseTemplateCLI(templatesList)
        return
    }

    return templateId
}

def loadTemplatesList(Path pathToTemplatesFolder) {
    templatesList = [:]

    File currentFolder = new File(pathToTemplatesFolder.toString())

    id = 0

    for(File folderElement : currentFolder.listFiles()){
        if((folderElement.isFile()) && (folderElement.getName().contains('.json'))){
            templatesList.put(id, folderElement.getName())

            id++
        }
    }

    return templatesList
}

def insertValues(Template loadedTemplate) {
    def insertations = loadedTemplate.getInsertations()
    def insertationsMap = loadedTemplate.getInsertations().getInsertationsMap()

    printAppOutput(text = "Input values", is_title=false, level=2, prefix="[", postfix="]?: ", needNewLine=true)

    for(Insertation insertation : insertationsMap.keySet()){
        def answerIsRight = false

        while(!answerIsRight){
            def answer = createCLIQuestion(insertation.getDescription(), false)

            answerIsRight = insertations.put(insertation, answer)
        }
    }
}

def clearTemplatesFolder() {
    executeCommand(
        [
            "rm",
            "-r",
            "./Templates",
        ].join(" ")
    )
}

def String createCLIQuestion(String questionText, Boolean isAppQuestion) {
    if(isAppQuestion){
        printAppOutput(text=questionText, is_title=false, level=1, prefix="", postfix=": ", needNewLine=false)
    } else {
        printAppOutput(text=questionText, is_title=false, level=2, prefix="[", postfix="]?: ", needNewLine=false)
    }

    return System.in.newReader().readLine()
}

def executeCommands(String[] commands) {
    unionCommand = ""

    unionCommand = commands.join(";")

    executeCommand(unionCommand)
}

def executeCommand(String command) {
    Path currentDir = Paths.get((new File(".")).getAbsolutePath())

    def commandWrapper = [
        'cd',
        """\"${currentDir.toString()}\"""",
        ';',
        command,
    ].join(' ')

    def executer = [
        'bash',
        '-c',
        '\'',
        commandWrapper.toString(),
        '\'',
    ].join(' ')

    Process process = executer.execute()

    new StringWriter().with {
         o -> new StringWriter().with {
            e -> process.waitForProcessOutput(o, e)
            [ o, e ]*.toString()
         }
    }
}

app()
