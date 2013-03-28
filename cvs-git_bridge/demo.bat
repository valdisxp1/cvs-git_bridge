java -jar target/cvs-git_bridge-1.0-SNAPSHOT.jar init
java -jar target/cvs-git_bridge-1.0-SNAPSHOT.jar cvsimport -d test/cvsroot multibranchtest
git clone git/ git_clone/
pause