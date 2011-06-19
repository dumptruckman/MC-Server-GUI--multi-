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

    if (isset($_POST['command'])) {
        if ($_POST['command'] == 'input') {
            sendInput($_POST['input']);
        }
        if ($_POST['command'] == 'start') {
            startServer();
        }
        if ($_POST['command'] == 'stop') {
            stopServer();
        }
    }

    echo '<script src="http://code.jquery.com/jquery-latest.js"></script>';
    echo '<div id="consoleout" style="overflow: auto; width: 800px; height: 200px;">';

    $response = getOutput();
    if(isset($response['Error'])) {
        print "Error: " . $response['Error'];
    } else if(isset($response['Success'])) {
        echo $response['Success'];
    } else {
        echo $response;
    }
    echo '</div>';
?>
<script>
    $(function(){
        $('#submitinput').click(function(){
            $('#consoleout').html('<img src="http://www.locrete.com/images/ajax-loader.gif" />');
            $.ajax({
                url:$('#consoleout').val(),
                type:'POST',
                data:{ command:'input', input:$('#input').val() },
                success: function(data){
                    $('#consoleout').html(data);
                }
            });
            return false;
        });
        $('#start').click(function(){
            $('#consoleout').html('<img src="http://www.locrete.com/images/ajax-loader.gif" />');
            $.ajax({
                url:$('#consoleout').val(),
                type:'POST',
                data:{ command:'start' },
                success: function(data){
                    $('#consoleout').html(data);
                }
            });
            return false;
        });
        $('#stop').click(function(){
            $('#consoleout').html('<img src="http://www.locrete.com/images/ajax-loader.gif" />');
            $.ajax({
                url:$('#consoleout').val(),
                type:'POST',
                data:{ command:'stop' },
                success: function(data){
                    $('#consoleout').html(data);
                }
            });
            return false;
        });
    });
</script>
<?php
    echo '  <script>
                $(function(){
                    $("#consoleout").prop({ scrollTop: $("#consoleout").prop("scrollHeight") });
                });
            </script>';
    //echo '  <form id="servercontrol" method="post" action="#">
    echo       '<ul>';
                    //<textarea name="Output" cols="75" rows="10">';
    
    //echo           '</textarea>
    echo           '<li>
                        <label>Input: </label>
                        <div>
                            <input id="input" name="input" type="text" value=""/>
                            <input type="hidden" name="form_id" value="192599" />
                            <input id="submitinput" class="button_text" type="submit" name="submitinput" value="Submit" />
                        </div>
                    </li>
                    <li class="buttons">
                        <input type="submit" id="start" name="start" value="Start Server">
                    </li>
                    <li class="buttons">
                        <input type="submit" id="stop" name="stop" value="Stop Server">
                    </li>

                </ul>';
            //</form>';
?>