<?php

include ("settings.php");
include("conn_masterdb_open.php");
//fetch schema
if (isset($_POST['tablename']))
    $sql = "select CREATE_TIME from TABLES where table_schema='{$mysql_database}' and table_name='{$_POST['tablename']}';";
else
    $sql = "select TABLE_NAME,CREATE_TIME from TABLES where table_schema='{$mysql_database}' order by TABLE_NAME;";
$result = mysql_query($sql);
$rows = mysql_fetch_array($result);
if (!$rows) {
    echo "SyncError: No this database";
    return;
} else if (isset($_POST['tablename'])) {
    echo $rows['CREATE_TIME'];
} else {
    $result = mysql_query($sql);
    $output;
    while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
        $output.=$row['TABLE_NAME'] . "@c@" . $row['CREATE_TIME'] . "@r@";
    }
    $output = substr($output, 0, -3);
    echo $output;
    include("conn_close.php");
}
?>