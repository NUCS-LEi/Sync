<?php

include ("settings.php");
include("conn_open.php");
//fetch table schema
$sql = "select Version from version;";
$result = mysql_query($sql);
$rows = mysql_fetch_array($result);
if (!$rows) {
    echo "SyncError: No version information in database";
    return;
} else {
    echo $rows['Version'];
}
include("conn_close.php");
?>