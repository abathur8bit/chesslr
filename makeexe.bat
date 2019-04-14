@echo off
@if "%1" == "" goto usage
javapackager -deploy -title "ChessLR" -name "ChessLR" -appclass com.axorion.chessboard.ChessLR -native exe -Bicon=icon.ico -outdir dist -outfile ChessLR.app -srcfiles chesslr-%1.jar

ren dist\bundles\ChessLR-1.0.exe ChessLR-%1.exe
goto end

:usage
echo makeexe.bat version

:end
