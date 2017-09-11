<?php

$conn = mysql_connect($mysql_server_name, $mysql_username, $mysql_password, $mysql_database);
mysql_select_db($mysql_database, $conn);
?>