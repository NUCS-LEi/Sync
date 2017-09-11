<?php

if (isset($_POST['phoneID']) && isset($_POST['tableName'])) {
    include ("settings.php");
    include("conn_open.php");
    $lastSyncTime = date('Y-m-d H:i:s');
    $sql = "INSERT INTO sync_time (PhoneID, TableName,LastSyncTime) VALUES ('{$_POST['phoneID']}','{$_POST['tableName']}', '" . $lastSyncTime . "') ON DUPLICATE KEY UPDATE LastSyncTime = '" . $lastSyncTime . "';";
    mysql_query($sql);
    echo $lastSyncTime;
    include("conn_close.php");
}
?>