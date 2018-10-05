@idljava.pro

H = start_idljava()
jSampHub = H['hub']
jMsgHandler = H['msgHandler']

; be aware that as of the current version, index "layers" is an array with one element
; direct access requires brackets, e.g.: (n['layers'])[0]
print, "waiting for notification from SAMP-Hub..."
n = wait_for_notification(jMsgHandler)
print, "done."
END