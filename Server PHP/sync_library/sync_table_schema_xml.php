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
        echo "<schema>";
        $last_table_name = "";
        while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
            if ($last_table_name != "" && $row['TABLE_NAME'] != $last_table_name)
                echo "</table>";
            if ($row['TABLE_NAME'] != $last_table_name) {
                echo "<table>";
                echo "<name>";
                echo $row['TABLE_NAME'];
                echo "</name>";
            }
            echo "<column>";
            echo "<column_name>";
            echo $row['COLUMN_NAME'];
            echo "</column_name>";
            echo "<column_default>";
            echo $row['COLUMN_DEFAULT'];
            echo "</column_default>";
            echo "<is_nullable>";
            echo $row['IS_NULLABLE'];
            echo "</is_nullable>";
            echo "<data_type>";
            echo $row['DATA_TYPE'];
            echo "</data_type>";
            echo "<column_key>";
            echo $row['COLUMN_KEY'];
            echo "</column_key>";
            echo "</column>";
            $last_table_name = $row['TABLE_NAME'];
        }
        echo "</table>";
        echo "</schema>";
        include("conn_close.php");
    }
}
?>