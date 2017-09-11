<?php

if (isset($_POST['query'])) {
    include ("settings.php");
    include("conn_open.php");
    $result = mysql_query($_POST['query']);
    while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
        foreach ($row as $value) {
            $output.=$value;
            $output.="@c@";
        }
        $output = substr($output, 0, -3);
        $output.="@r@";
    }
    $output = substr($output, 0, -3);
    echo $output;
    include("conn_close.php");
} else {
    echo "Error: No query statement passed";
}
?>