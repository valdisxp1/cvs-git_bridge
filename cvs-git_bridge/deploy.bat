start mvn package -DskipTests
pause
IF NOT EXIST ../../deployer mkdir ../../deployer
cp target/cvs-git_bridge-1.0-SNAPSHOT.jar ../../deployer
pause