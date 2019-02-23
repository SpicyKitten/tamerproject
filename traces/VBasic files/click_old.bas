Attribute VB_Name = "Module1"
Private Sub CommandButton1_Click()
Dim LastRow As Long
Dim StartCell As Range
Dim maxEpisode As Long
Dim CurrRow As Long
Dim IncorrectCount As Long
Dim CellCount As Long
Dim minEp As Long
Dim maxEp As Long
Dim epReward As Long
autoFill = WorksheetFunction.CountA(Columns(16)) = 0
Set StartCell = Range("$A$3")
LastRow = StartCell.SpecialCells(xlCellTypeLastCell).Row
For i = 3 To LastRow
If Cells(i, 3).Value <> Cells(i, 4).Value Then
Cells(i, 6).Value = 1
Else
Cells(i, 6).Value = 0
End If
Next i
Cells(3, 9).Value = CStr(LastRow)
maxEpisode = WorksheetFunction.Max(Range("$A:$A"))
Cells(7, 8).Value = maxEpisode
If autoFill Then
Cells(1, 16).Value = "AutoFilled"
Cells(2, 16).Value = 1
Cells(2, 17).Value = 5
Cells(3, 16).Value = 6
Cells(3, 17).Value = 10
Cells(4, 16).Value = 11
Cells(4, 17).Value = 15
Cells(5, 16).Value = 16
Cells(5, 17).Value = 20
Cells(6, 16).Value = 21
Cells(6, 17).Value = 25
Cells(7, 16).Value = 26
Cells(7, 17).Value = 30
Cells(8, 16).Value = 31
Cells(8, 17).Value = 35
Cells(9, 16).Value = 36
Cells(9, 17).Value = 40
Cells(10, 16).Value = 41
Cells(10, 17).Value = 45
Cells(11, 16).Value = 46
Cells(11, 17).Value = 50
For i = 1 To maxEpisode
Cells(i + 11, 16).Value = i
Cells(i + 11, 17).Value = i
Next i
End If
Cells(1, 6).Value = "ErrPredict"
Cells(1, 7).Value = "Agent"
Cells(1, 8).Value = "Agent_Actions"
Cells(2, 7).Value = "#0"
Cells(3, 7).Value = "#1"
Cells(2, 8).Value = "=COUNTIF($B$3:OFFSET($B$3,0,0,($I$3)-2,1),G2)"
Cells(3, 8).Value = "=COUNTIF($B$3:OFFSET($B$3,0,0,($I$3)-2,1),G3)"
Cells(4, 8).Value = "Total_Actions"
Cells(5, 8).Value = "=SUM(H2, H3)"
Cells(6, 8).Value = "Num_Episodes"
Cells(2, 3).Value = "=SUM(C3:OFFSET(C3,0,0,($I$3)-2,1))"
Cells(2, 4).Value = "=SUM(D3:OFFSET(D3,0,0,($I$3)-2,1))"
Cells(2, 5).Value = "=SUM(E3:OFFSET(E3,0,0,($I$3)-2,1))"
Cells(2, 6).Value = "=SUM(F3:OFFSET(F3,0,0,($I$3)-2,1))"
Cells(2, 9).Value = "Num_Rows"
Cells(4, 9).Value = "% Immoral Actions"
Cells(5, 9).Value = "=$D$2/$H$5"
CurrRow = 3
For i = 1 To maxEpisode
IncorrectCount = 0
CellCount = 0
epReward = 0
Cells(i + 1, 10) = i
Do Until Cells(CurrRow, 1).Value <> i
CellCount = CellCount + 1
stepVal = Cells(CurrRow, 5).Value
If stepVal <> 0 Then
    epReward = epReward + stepVal
End If
If Cells(CurrRow, 6).Value = 1 Then
    IncorrectCount = IncorrectCount + 1
End If
CurrRow = CurrRow + 1
Loop
Cells(i + 1, 11).Value = IncorrectCount
Cells(i + 1, 12).Value = CellCount
Cells(i + 1, 13).Value = epReward
Next i
numRows = WorksheetFunction.CountA(Columns(16)) + 1
If Cells(1, 16).Value <> "" Then
numRows = numRows - 1
End If
For k = 2 To numRows
    minEp = Cells(k, 16).Value
    maxEp = Cells(k, 17).Value
    Cells(k, 14).Value = 0
    Cells(k, 15).Value = 0
    Cells(k, 19).Value = 0
    For i = minEp To maxEp
        Cells(k, 14).Value = Cells(k, 14).Value + Cells(i + 1, 11).Value
        Cells(k, 15).Value = Cells(k, 15).Value + Cells(i + 1, 12).Value
        Cells(k, 19).Value = Cells(k, 19).Value + Cells(i + 1, 13).Value
        Cells(k, 18).Value = ""
    Next i
Next k
Cells(2, 18).Value = "=(O2-N2)/O2"
Cells(numRows + 1, 18).Value = "End"
Cells(1, 17).Value = "Proportion Correct"
Cells(1, 19).Value = "Cumulative Reward"
End Sub



