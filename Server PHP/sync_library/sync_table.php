<?php

if (isset($_POST['tablename']) && isset($_POST['phoneID']) && isset($_POST['upsyncstring'])) {
    $page_records = 1000;
    $row_array = array();
    $table_schema = array();
    $table_keys = array();
    if (isset($_POST['page']))
        $page = $_POST['page'];
    else
        $page = 0;
    if ($start < 0)
        $start = 0;

    include ("settings.php");
    include("conn_masterdb_open.php");
    //fetch table schema
    $sql = "select column_name,column_key from columns where table_schema='{$mysql_database}' and table_name='{$_POST['tablename']}';";
    $result = mysql_query($sql);
    $rows = mysql_fetch_array($result);
    if (!$rows) {
        echo $sql;
        return;
    } else {
        $result = mysql_query($sql);
        while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
            //put column_names into array
            $table_schema[] = $row['column_name'];
            if ($row['column_key'] == 'PRI')
            //put key_column_names into array
                $table_keys[] = $row['column_name'];
        }
        //assume all column are keys if a table doesn't have any key
        if (count($table_keys) == 0)
            $table_keys = $table_schema;
    }
    include("conn_close.php");
    include("conn_open.php");

    ////////////////////////Sync client-server direction////////////////////////////////
    //parse rows with data string from client
    $row = explode('@r@', $_POST['upsyncstring']);
    foreach ($row as $_row) {
        $col_array = array();
        //organize query, insert and update SQL statements
        $query_sql = "select LastModDate from {$_POST['tablename']}";
        $update_sql = "update {$_POST['tablename']} set ";
        $update_statement = "";
        $insert_sql = "insert into {$_POST['tablename']} values(";
        $insert_statement = "";
        $where_condition = " where ";
        $value = explode('@c@', $_row);
        foreach ($value as $_value) {
            $col_array[] = $_value;
        }
        for ($i = 0; $i < count($table_schema); $i++) {
            $key_flag = 0;
            for ($j = 0; $j < count($table_keys); $j++) {
                if ($table_keys[$j] == $table_schema[$i]) {
                    $key_flag = 1;
                    $where_condition.=$table_keys[$j] . "='" . $col_array[$i] . "' and ";
                }
            }
            if ($key_flag == 0)
                $update_statement.=$table_schema[$i] . "='" . $col_array[$i] . "',";
            $insert_statement.="'" . $col_array[$i] . "',";
        }
        $insert_statement = substr($insert_statement, 0, -1) . ")";
        $update_statement = substr($update_statement, 0, -1);
        $where_condition = substr($where_condition, 0, -5);
        $query_sql.=$where_condition;
        $insert_sql.=$insert_statement;
        $update_sql.=$update_statement . $where_condition;
        $row_array[] = $col_array;
        //check if record exist in table
        $result = mysql_query($query_sql);
        $rows = mysql_fetch_array($result);
        if (!$rows) {
            //if not exist, then insert it
            mysql_query($insert_sql);
        } else {
            if (date('Y-m-d H:i:s', strtotime($col_array[count($col_array) - 1])) > date('Y-m-d H:i:s', strtotime($rows['LastModDate']))) {
                //if exist, check if the record from client has later modified date, if has, then update
                mysql_query($update_sql);
            }
        }
    }

    ////////////////////////Sync server-client direction////////////////////////////////
    //Set limit offset
    $start = $page * $page_records;
    $limit_sql = " limit " . $start . "," . $page_records . ";";
    $sync_time_sql = "select LastSyncTime from sync_time where PhoneID='{$_POST['phoneID']}' and TableName='{$_POST['tablename']}'";
    $result = mysql_query($sync_time_sql);
    $rows = mysql_fetch_array($result);
    if (!$rows)
        $sql = "select * from {$_POST['tablename']}";
    else
        $sql = "select * from {$_POST['tablename']} where LastModDate>'" . $rows['LastSyncTime'] . "'";
    $sql = $sql . $limit_sql;
    $result = mysql_query($sql);
    $output = "";
    $count = 0;
    while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
        foreach ($row as $value) {
            $output.=$value;
            $output.="@c@";
        }
        $output = substr($output, 0, -3);
        $output.="@r@";
        $count++;
    }
    $output = substr($output, 0, -3);
    //count==page_records means is not the last page
    if ($count == $page_records)
        $output.="@p@";
    echo $output;
    include("conn_close.php");
} else {
    echo "SyncError: Unsufficent Parameters";
}
?>