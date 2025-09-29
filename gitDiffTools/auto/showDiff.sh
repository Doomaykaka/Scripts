cd "$(dirname "${BASH_SOURCE[0]}")"
path=$(find ~+ -type f -name "*.diff" | head -1)
echo "${path}"
mkdir TMP
cd TMP
git init
cp "${path}" .
cat *
read
cd ..
rm -rf TMP