<?php
    $GLOBALS['password'] = 'password';
    $GLOBALS['port'] = 42424;
    $GLOBALS['ip'] = 'localhost';

    function startServer() {
        return sendToGui(json_encode(array('Password' => $GLOBALS['password'], 'Request' => 'Start Server', 'Data' => array(''))));
    }

    function stopServer() {
        return sendToGui(json_encode(array('Password' => $GLOBALS['password'], 'Request' => 'Stop Server', 'Data' => array(''))));
    }

    function getOutput() {
        return sendToGui(json_encode(array('Password' => $GLOBALS['password'], 'Request' => 'Get Output', 'Data' => array(''))));
    }

    function sendInput($input) {
        return sendToGui(json_encode(array('Password' => $GLOBALS['password'], 'Request' => 'Send Input', 'Data' => array($input))));
    }

    function sendToGui($json) {
        $sock = fsockopen($GLOBALS['ip'] = 'localhost', $GLOBALS['port']);
        $written = fwrite($sock, $json);
        if ($written != false) {
            fflush($sock);
            $response = json_decode(fgetss($sock), true);
            fclose($sock);
            return $response;
        } else {
            return "Error processing request";
        }
    }

    if (isset($_POST['submit'])) {
        sendInput($_POST['input']);
    }
    if (isset($_POST['start'])) {
        startServer();
    }
    if (isset($_POST['stop'])) {
        stopServer();
    }



    echo '  <form method="post" action="http://gnarbros.dyndns.org/node/11">
                <ul>
                    <textarea name="Output" cols="75" rows="10">';
    $response = getOutput();
    if(isset($response['Error'])) {
        print "Error: " . $response['Error'];
    } else if(isset($response['Success'])) {
        echo $response['Success'];
    } else {
        echo $response;
    }
    echo            '</textarea>
                    <li>
                        <label>Input: </label>
                        <div>
                            <input id="input" name="input" type="text" value=""/>
                            <input type="hidden" name="form_id" value="192599" />
                            <input id="saveForm" class="button_text" type="submit" name="submit" value="Submit" />
                        </div>
                    </li>
                    <li class="buttons">
                        <input type="submit" name="start" value="Start Server">
                        <input type="submit" name="stop" value="Stop Server">
                    </li>
                </ul>
            </form>';
?>