<?php

if (isset($_POST['phoneID'])) {
    include ("settings.php");
    include("conn_open.php");
    if (isset($_POST['tablename']) && $_POST['tablename'] != null && $_POST['tablename'] != "")
        $sql = "delete from sync_time where PhoneID='{$_POST['phoneID']}' and TableName='{$_POST['tablename']}';";
    else
        $sql = "delete from sync_time where PhoneID='{$_POST['phoneID']}';";
    mysql_query($sql);
    echo "reset";
    include("conn_close.php");
}
?>