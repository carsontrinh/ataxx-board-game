# ataxx-board-game
A command-line strategy game, featuring a minimax AI. New to Ataxx? Learn the rules [here](https://en.wikipedia.org/wiki/Ataxx#Game_play).

## How to get started
Navigate to `ataxx-board-game/` and run `java -ea ataxx.Main`.
## Start/End Commands
* **clear** Abandons the current game, resets the board, and enters the setup state.
* **start** Enters the playing state and begins the game. First player is determined by where the setup left off (`red` player moves first with the default configuration).
* **quit** Abandons the current game and exits the program.
## Setup Commands
* **auto P** Enables the AI for player *P* (`red` or `blue`). The `blue` player defaults to an AI on initialization and after the `clear` command.
* **manual P** Disables the AI for player *P* (`red` or `blue`). The `red` player defaults to a manual player on initialization and after the `clear` command.
* **block CR** Sets a block at square (*C*, *R*) and at all squares reflecting across the middle row and column.
* **seeding N** Sets the random seed for the AI to *N*, a long integer.
## Moving
Moves may be made in the setup or playing state. Use the notation `c0r0-c1r1`, where `c0r0` denotes the position of the piece to move from and `c1r1` denotes the position to jump or extend to. Use a single hyphen `-` to denote a pass when no legal move is available.
## Miscellaneous Commands
* **help** Displays a brief summary of the commands.
* **dump** For debugging and testing purposes.
* **load F** Reads the given file *F*, substituting its contents as input into the program.
