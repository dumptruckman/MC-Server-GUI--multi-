<?php
    /*
     * PHP JSON API for MC Server GUI
     * Version 2.1
     *
     * Change Log
     * Version .2.1
     *      Now uses php constants instead of globals for ip, port, pass.
     *      The functions now handle the responses a bit more for you.  They
     *              will return the data section of a response if successful or
     *              the error message when not.  If Data is not present when
     *              successful, it will instead return the success message.
     *
     */

    // Use something like these to pass in the ip, port, password
    //define('PASSWORD', $_POST['pass']);
    //define('IP', $_POST['ip']);
    //define('PORT', $_POST['port']);

    // This function will start the server
    function startServer() {
        $response = sendToGui(array('Password' => PASSWORD, 'Request' => 'Start Server'));
        return handleResponse($response);
    }

    // This function will stop the server
    function stopServer() {
        $response = sendToGui(array('Password' => PASSWORD, 'Request' => 'Stop Server'));
        return handleResponse($response);
    }

    /* This function will retrieve the output from the server WITH the html formatting (it will keep all the colors, etc)
     * if $alltags is true or will retrieve the output with only <br> tags if false.
     */
    function getOutput($alltags = true) {
        if ($alltags) {
            $response = sendToGui(array('Password' => PASSWORD, 'Request' => 'Get Output'));
        } else {
            $response = sendToGui(array('Password' => PASSWORD, 'Request' => 'Get Output', 'Data' => array('Line Break Only')));
        }
        return handleResponse($response);
    }

    // This function will return plain text output with your choice of line end, or CRLF by default
    function getPlainTextOutput($lineend = 'CRLF') {
        $response = sendToGui(array('Password' => PASSWORD, 'Request' => 'Get Output', 'Data' => array('Plain Text', $lineend)));
        return handleResponse($response);
    }

    // This function allows you to send input to the server. example: sendInput("say Hello, server.");
    function sendInput($input) {
        $response = sendToGui(array('Password' => PASSWORD, 'Request' => 'Send Input', 'Data' => array($input)));
        return handleResponse($response);
    }

    // Executes task by specified name
    function executeTask($taskname) {
        $response = sendToGui(array('Password' => PASSWORD, 'Request' => 'Execute Task', 'Data' => array($taskname)));
        return handleResponse($response);
    }

    // Returns an array of task names
    function getTaskNames() {
        $response = sendToGui(array('Password' => PASSWORD, 'Request' => 'Get Task Names'));
        return handleResponse($response);
    }

    // Returns details of specified task
    function getTaskDetails($taskname) {
        $response = sendToGui(array('Password' => PASSWORD, 'Request' => 'Get Task Details', 'Data' => array($taskname)));
        return handleResponse($response);
    }

    // This function sends the json data to the server and returns an associative array containing the response or just a string if there was an error.
    function sendToGui($json) {
        $sock = fsockopen(IP, PORT);
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

    // This will handle the response from the server and either return that
    // successfully obtained data or return any error messages received.
    function handleResponse($response) {
        if (isset($response['Success']) or isset($response['Partial Success'])) {
            if (isset($response['Data'])) {
                return $response['Data'];
            } else {
                if (isset($response['Success'])) {
                    return $response['Success'];
                }
                if (isset($response['Partial Success'])) {
                    return $response['Partial Success'];
                }
            }
        } else if (isset($response['Error'])) {
            return $response['Error'];
        }
        return 'Unexpected Response';
    }

?>