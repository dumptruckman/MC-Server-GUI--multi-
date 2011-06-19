<?php
    // Change to the password you indicate in the GUI
    $GLOBALS['password'] = 'password';
    // In most cases this should stay as localhost
    $GLOBALS['ip'] = 'localhost';
    // Change to the port set in the GUI for the Web Interface
    $GLOBALS['port'] = 42424;

    // This function will start the server
    function startServer() {
        return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Start Server', 'Data' => array('')));
    }

    // This function will stop the server
    function stopServer() {
        return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Stop Server', 'Data' => array('')));
    }

    // This function will retrieve the output from the server WITH the html formatting (it will keep all the colors, etc)
    function getOutput() {
        return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Get Output', 'Data' => array('')));
    }

    // This function allows you to send input to the server. example: sendInput("say Hello, server.");
    function sendInput($input) {
        return sendToGui(array('Password' => $GLOBALS['password'], 'Request' => 'Send Input', 'Data' => array($input)));
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
     * } else if(isset($response['Success'])) {
     *    echo $response['Success'];
     * } else {
     *    echo $response;
     * }
     */
?>