$command = array('command' => update, 'id' => 12);
$sock = fsockopen('localhost', 3000);
fwrite($sock, json_encode($command));
$response = json_decode(fread($sock));
fclose($sock);
if(isset($response['error'])) {
  // handle error message
} else {
  // handle response
}

{ command: 'delete', id: 15 }

{ error: 'unknown command' }