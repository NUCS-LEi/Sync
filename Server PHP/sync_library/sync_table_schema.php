<?php

if (isset($_POST['tablename'])) {
    include ("settings.php");
    include("conn_masterdb_open.php");
//fetch schema
    $sql = "select TABLE_NAME,COLUMN_NAME,COLUMN_DEFAULT,IS_NULLABLE,DATA_TYPE,COLUMN_KEY from COLUMNS where table_schema='{$mysql_database}' and TABLE_NAME='{$_POST['tablename']}';";
    $result = mysql_query($sql);
    $rows = mysql_fetch_array($result);
    if (!$rows) {
        echo "SyncError: No this database";
        return;
    } else {
        $result = mysql_query($sql);
        $output="";
        while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
            $output.=$row['TABLE_NAME']."@c@";
            $output.=$row['COLUMN_NAME']."@c@";
            $output.=$row['COLUMN_DEFAULT']."@c@";
            $output.=$row['IS_NULLABLE']."@c@";
            $output.=$row['DATA_TYPE']."@c@";
            $output.=$row['COLUMN_KEY']."@r@";
        }
        $output = substr($output, 0, -3);
        echo $output;
        include("conn_close.php");
    }
}
?>