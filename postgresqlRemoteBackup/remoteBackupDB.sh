sudo_pass=$1
postgres_pass=$2

ssh login@address << EOF
cd ~
echo "${sudo_pass}" | sudo -S echo
echo "${postgres_pass}"| sudo pg_dump -U postgres_user database_name > backup_name.sql
EOF

scp login@address:~/backup_name.sql .
