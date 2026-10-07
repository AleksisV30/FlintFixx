; Ask once (not on auto-updates) whether to put a FlintFix Client shortcut on the desktop.
!macro customInstall
  ${ifNot} ${isUpdated}
    IfSilent flintfix_skip_shortcut
    MessageBox MB_YESNO|MB_ICONQUESTION "Create a FlintFix Client shortcut on the desktop?" /SD IDNO IDNO flintfix_skip_shortcut
    CreateShortCut "$DESKTOP\${SHORTCUT_NAME}.lnk" "$appExe" "" "$appExe" 0
    flintfix_skip_shortcut:
  ${endIf}
!macroend

!macro customUnInstall
  ${ifNot} ${isUpdated}
    Delete "$DESKTOP\${SHORTCUT_NAME}.lnk"
  ${endIf}
!macroend
