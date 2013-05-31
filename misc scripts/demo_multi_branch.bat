java -jar cvs-git_bridge-0.1.jar init
java -jar cvs-git_bridge-0.1.jar cvsimport -d test/cvsroot multibranchtest
git clone --mirror git/ git_clone.git/
mkdir git_clone
mv git_clone.git git_clone/.git
cd git_clone
git config core.bare false
git reset --hard
pause