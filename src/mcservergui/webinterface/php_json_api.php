<?php
    /*
     * PHP JSON API for MC Server GUI
     * Version 2.0
     */

    // Change to the password you indicate in the GUI
    //$GLOBALS['password'] = 'password';
    // In most cases this should stay as localhost
    //$GLOBALS['ip'] = 'localhost';
    // Change to the port set in the GUI for the Web Interface
    //$GLOBALS['port'] = 42424;

    // This function will start the server
    function startServer() {
        return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Start Server'));
    }

    // This function will stop the server
    function stopServer() {
        return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Stop Server'));
    }

    /* This function will retrieve the output from the server WITH the html formatting (it will keep all the colors, etc)
     * if $alltags is true or will retrieve the output with only <br> tags if false.
     */
    function getOutput($alltags = true) {
        if ($alltags) {
            return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Get Output'));
        } else {
            return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Get Output', 'Data' => array('Line Break Only')));
        }
    }

    // This function will return plain text output with your choice of line end, or CRLF by default
    function getPlainTextOutput($lineend = 'CRLF') {
        return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Get Output', 'Data' => array('Plain Text', $lineend)));
    }

    // This function allows you to send input to the server. example: sendInput("say Hello, server.");
    function sendInput($input) {
        return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Send Input', 'Data' => array($input)));
    }

    // Executes task by specified name
    function executeTask($taskname) {
        return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Execute Task', 'Data' => array($taskname)));
    }

    // Returns an array of task names
    function getTaskNames() {
        return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Get Task Names'));
    }

    // Returns details of specified task
    function getTaskDetails($taskname) {
        return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Get Task Details', 'Data' => array($taskname)));
    }

    // This function sends the json data to the server and returns an associative array containing the response or just a string if there was an error.
    function sendToGui($json) {
        $sock = fsockopen($GLOBALS['ip'], $GLOBALS['port']);
        if ($sock != false) {
            $written = fwrite($sock, json_encode($json));
            if ($written != false) {
                fflush($sock);
                $response = json_decode(fgets($sock), true);
                fclose($sock);
                return $response;
            } else {
                return "Error processing request";
            }
        } else {
            return "Error opening connection";
        }
    }

    /* The response array contains 1 key/value pair.  The key will either be Success or Error, and the value will be the response.
     * The only command you really need to read the response from is getOutput().
     * Here is an example of retrieving the server output:
     *
     * $response = getOutput();
     * if(isset($response['Error'])) {
     *    print "Error: " . $response['Error'];
     * } else if(isset($response['Data'])) {
     *    echo $response['Data'];
     * } else {
     *    echo $response;
     * }
     */
?>