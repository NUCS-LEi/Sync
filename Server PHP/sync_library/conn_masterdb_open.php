<?php 
	$conn=mysql_connect($mysql_server_name,$mysql_username,$mysql_password,$mysql_master_database); 
	mysql_select_db($mysql_master_database,$conn); 
?>