java -jar target/cvs-git_bridge-0.1.jar init
java -jar target/cvs-git_bridge-0.1.jar cvsimport -d test/cvsroot multibranchtest
git clone git/ git_clone/
pause