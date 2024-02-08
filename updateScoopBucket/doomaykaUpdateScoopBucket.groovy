import java.util.logging.Level
import java.util.logging.Logger
import java.nio.file.Path
import java.nio.file.Paths

logger = Logger.getLogger('Logger')

def app(){
    logger.log(Level.INFO, 'Hello')

    updateScoopBucket()

    gitPushing()

    logger.log(Level.INFO, 'Done!')
}

def updateScoopBucket(){
    logger.log(Level.INFO, 'Scoop bucket updating')

    //powershell -File bin/checkver.ps1 -Update -App "*" 2> /dev/null
    def updateScoopBucketScript = [
        "bin/checkver.ps1",
        "-Update",
        "-App",
        "\"*\"",
    ].join(' ')

    def updateScoopBucketExecuter = [
        "powershell",
        "-File",
        updateScoopBucketScript,
        "2>",
        "/dev/null",
    ].join(' ')

    executeCommand(updateScoopBucketExecuter)
}

def gitPushing(){
    logger.log(Level.INFO, 'Pushing changes made to the scoop bucket to git')

    def gitAddToIndex = [
        "git",
        "add",
        ".",
    ].join(' ')

    def gitCommit = [
        "git",
        "commit",
        "-m",
        "\"Scoop bucket updated\"",
    ].join(' ')

    def gitPush = [
        "git",
        "push",
    ].join(' ')

    def gitUpdateCommand = [
        gitAddToIndex,
        ";",
        gitCommit,
        ";",
        gitPush,
    ].join(' ')

    executeCommand(gitUpdateCommand)
}

def executeCommand(String command){
    logger.log(Level.INFO, 'Executing a command :' + command)

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
