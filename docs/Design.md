# Bridge Design

## Duplicate Screens

### Game View

`#/duplicate/<dupid>`

Shows duplicate score view, only complete games.  

Buttons
- Table <n>
- Director


### Director's View

`#/duplicate/<dupid>/director`

Shows duplicate score view, the director's view  

Buttons
- Board Buttons - goes to Board Director's View
- Back - goes back to Game View

### Table View

`#/duplicate/<dupid>/table/<n>`

Shows the table (round, NS, EW, boards)

Buttons
- Round buttons for any rounds that have unplayed boards on this table.
- Overview, goes to Game View

### Round View

`#/duplicate/<dupid>/table/<n>/round/<n>`

Shows duplicate score view from table perspective

Buttons
- Board <n> for all boards in the round, goes to Hand View
- Overview, goes to Game View

### Hand View

`#/duplicate/<dupid>/table/<n>/board/<n>/hand/<n>`

For entering a hand and result of hand

### Board View

`#/duplicate/<dupid>/table/<n>/board/<n>`

Showing the board results, from the active perspective (table).

Buttons
- To Round View(table)
- To Hand View for each hand played
- Overview, goes to Game View

### Board Director's View

`#/duplicate/<dupid>/director/board/<n>`

Showing the board results, from the director's perspective

Buttons
- To Hand View for each hand played
- Overview, goes to Director's View

## Duplicate State Machine

```
Game View -> Table View (table) -> Round View (table) -> Hand View -> Board View (table) ---> Round View(table)
          \
           -> Director's View -> Board Director's View -> Hand View   
```
         