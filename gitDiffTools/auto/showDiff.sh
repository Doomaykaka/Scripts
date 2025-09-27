cd "$(dirname "${BASH_SOURCE[0]}")"
mkdir TMP
cd TMP
git init
cp "$1" .
cat *
read
cd ..
rm -rf TMP