# Eclipse Commander
Next generation command palette using [KAVI](https://github.com/dakaraphi/kavi) interfaces exceeding capabilities of typical sublime like command palettes

Current interfaces provided:
 * **Commander** - provides interface for selecting and executing all Eclipse commands.
 * **Finder** - provides interface for selecting and opening workspace resources.
 
# Background
## The problem
* In a very large complex application such as Eclipse, your workflow can be significantly slowed due to the massive complexity of the application.
* Eclipse environments can easily have well over 2000 commands which are executed through menus, hot keys, custom dialogs or buttons.
* Remembering where are the locations of menus, views, buttons or attempting to assign so many actions to hot keys proves to be impractical to impossible.
* Think time increases simply attempting to navigate and execute your intentions.
* Quick Access is the Eclipse attempt to address these issues, but fails to be an optimal implementation of a solution.
    * Modern fuzzy matching like sublime is not implemented requiring additional keystrokes 
    * Ranking is not optimal, partially due also to group sorting of actions by category which requires navigation further down the list
    * Reuse of recent actions is not implemented in an intuitive way
    * Not designed with fast keyboard interaction in mind 
 
## Goals
* Provide fastest workflow possible in a complex application
    * Interface should be faster than using mouse, menus, buttons, or even hot keys in most cases.
    * Typing flow should never be interrupted.
    * All actions should be possible without use of mouse.
    * Common used actions should have quicker access. 

# Optimum Experience
Once the working view is populated with commands that are used frequently, you should be able to experience the following benefits:

1. **Relevant Information:** your working view presents you with only items which you actually use.  This minimizes mental focus interuptions that subtly happen when presented with too much irrelevant visual information to consume.
2. **Focus on Recent:** items are ordered in the working view by last use.  Therefore to execute the last command again, simply launch the interface and press `enter`.  The first item will already be selected, no other key strokes required.  This feature can also be used in the `Finder` interface for navigating among recently viewed or opened files.
3. **Fast Interface Launch:** this is dependent on how you configure your keybindings.  The intent of the recommended key bindings is to provide an experience where you are not interupted in your typing flow to execute commands. `shift+enter` allows to launch the interface without moving your hands from natural typing position and immediately follow with typing into the input filter to select a command to execute.  Also, it becomes very natural to re-execute the last command with a very fast sequence of `shift+enter enter` which can be executed fast enough that you may not even see the interface fully appear.
4. **Fast Selections:** a combination of features allows for very fast selection and execution of commands.  Since your working view is contstrained to only those items useful to you, your working view likely consists of dozens of items vs. thousands that are available in the application.  You will therefore be able to select items with usually one or two keystrokes using first letter or acronym of a command.  Equal weighted matches in the working view are also sorted by recent.  So if you happen to have 5 different launch configurations, you can re-launch the last configuration simply by pressing `l` and `enter`.  Additionaly, the interface has a `fast select` feature allowing you to execute any item in the list without scrolling, cursor down, or using the mouse.

The Optimum Exerpience should look something like the following:

In this short clip, the time to launch and execute commands is subsecond. This is all done without hot keys.  Abreviations of the commands we use are easier to remember and faster to execute all without leaving our typing flow.  Since the default command list is our set of recent or favorites, the selections become predictable and often can be done utilizing only 1 or 2 characters.

![optimal](/readme-images/optimal.gif)

# Installation
## Eclipse Marketplace
[Marketplace Installer](https://marketplace.eclipse.org/content/commander)

## Recommended setup
All available commands available to key bindings can be found by going to the Eclipse key preferences and searching for `dakara` 
* Key Bindings - no default bindings are registered to avoid possible conflicts.  Below are recommended bindings.
    * Commander - `shift+enter` - This is one of the fastest launching bindings possible which also does not interrupt the typing flow.
    You will likely need to bind this for both `windows and dialogs` as well as `text editing`.  The exact terms and options differ for different platforms and plugins installed.
    * Finder - `shift+space` - This is another very fast binding, but for some this binding may be hit sometimes accidentally.  Otherwise, you might opt to replace the default `open resource` binding to use `Finder`

![key-bindings](/readme-images/key-bindings.gif)

* First time use
    * Launching `Commander` and `Finder` initially opens your `working` view which contains the list of items you have been using recently.  The first time you launch, these will be blank.
    * Press the `tab` key to switch to the `discovery` mode to find items.  
    * In a short period of time, the `working` view will be populated with items you commonly use and you will only infrequently need to switch to `discovery`

# KAVI implemented features for all interfaces 
 
## Term Matching

### Multi column
Default match will search across columns for matches.
![multi column](/readme-images/multi-column-match.gif)
### Specific column
Columns can be selected individual for matches by using a `,` to separate the column filters.
![specific column](/readme-images/specific-column-match.gif)
### Literal 
A space after the filter text will cause the preceding text to be matched literal instead of fuzzy.
![literal column](/readme-images/literal-match.gif) 
### Fuzzy multi word out of order
Contiguous characters matched using a fuzzy strategy that attempts to match words in any order.
A space separating words will force matching of the literal words also allowing for out of order matces.
![fuzzy column](/readme-images/fuzzy-out-of-order-match.gif)
### Quality filtering
One or two letters will not match in the middle of words.  This is done to prevent a long tail of low ranking matches.
![quality](/readme-images/quality-match.gif)
### Acronym
Fuzzy matching also will attempt to match by acronym.
You can force acronym pattern matching by preceding the input with a space.

![acronym column](/readme-images/acronym-match.gif)

## Ranking Sort
Items are sorted first by rank and then by name.
This allows for grouping of items by rank and easier identification of items within the ranked group.

![acronym column](/readme-images/ranking-sort.gif)

## Fast Select
Fast select allows list actions directly on target items without needing to navigate to the item with mouse or keyboard cursor.
This mode is enabled when typing `/` in the filter input.

### Immediate action invocation
With `Fast Select` enabled, typing the letters in the fast select guide next to the row immediately inititates that row action.

![acronym column](/readme-images/fast-select.gif)
### Multi select
`Fast Multi Select` allows fast selection of multiple items.  This mode is active when `//` is entered in the input field.

![acronym column](/readme-images/fast-multi-select.gif)

### Range select
A range can be selected by starting the row identifier with `-`.  The range will be applied from the last selected item and will use the selected state of the that same item.

![acronym column](/readme-images/fast-range-select.gif)

### Inverse select
Inverse select will inverse all selections currently in the filtered view.  
Inverse select is performed by pressing `!` after the fast select slashes `//`

### All select/deselect
If any items in the view are selected, this action will deselect all selected.  Otherwise, this same action will select all in the filtered view.
`All select/deselect` is performed by pressing `space` after the fast select slashses `//`

## Navigation
### List paging
`crtl+j` will page down in the list.
`ctrl+k` will page up in the list.

### Cursor movement in input field
`ctrl+j` will move to beginning of input field
`ctrl+l` will move to the end of the input field

## Working and Discovery Modes
`Working` is a view of the set of items that consist of favorites and or recently used items.
This view is intended to be the primary view that you would use most of the time and therefore is the default view.
However, this view does need to be primed before it is useful.  Over the course of a few days this view would accumulate actions or items you are currently using.

`Discovery` is a view of all possible items.  Selection of items from Discovery will add them to the recent list which makes them appear in the `Working` view.

### Switching modes
Press `TAB` to instantly switch view modes between `Working` and `Discovery`

![mode-toggle](/readme-images/mode-toggle.gif)

### Recent
Recent items are shown in the `Working` view.  The items are always sorted by most recent.  To reuse the last used item, simply open the dialog and press enter which will default to using the first item in the list.

### Favorites
Items can also be permanently added to the `Working` view.  These are considered favorite items.  They are also sorted in by most recently used in the same view as recent items.
A vertical marker bar appears to the left of items which are favorites.

![favorites](/readme-images/favorites.gif)

### Export/Import preferences
The `Working` set of items is contained within preferences and will be exported and imported with Eclipse preferences.

### Across workspaces
The `Working` set of items is stored in the global preferences store.  Therefore, your recently used commands will still be available across workspaces.

## Context Actions
Context actions are those actions that otherwise would require right clicking on an item to bring up another menu or dialog.
Context actions here are initiated using the `;` key.
The context actions will be performed on all selected items from the previous view.

![context](/readme-images/context.gif)

### Copy selected to clipboard
This action will copy all items in the selection to the clipboard.
![clipboard](/readme-images/clipboard.gif)

### View Selected
This action will toggle showing only the selected items in the view.  This allows you to type different input filters, select items and then finally see all the items you have selected at once before performing some action on those items.

### Favorites
Favorites can be added or removed through context actions.

### Sort
Items which are normally sorted by rank or sorted by most recent can be sorted by name using this context action.

![acronym column](/readme-images/context-sort.gif)

# Commander
Specific features for the `commander` interface
## Columns
1.  Name and description of the command
2.  Category of the command
## Launchers
Launch configurations are added to the list of available commands.
Currently, all launch configurations are run in debug mode by default.  In the future this may be configurable.
![acronym column](/readme-images/launcher.gif)
# Finder
Specific features for the `finder` interface
## Columns
1.  File name
2.  Project name
3.  File path within project
## Working
The recent list ordering of items in finder will be updated whenever you change editors in Eclipse.  
You can use this to always go back to previous file being editted after opening a view.  Just launch the finder and hit `enter` to go back to the last editor.

# Design and Technical Notes
## Fuzzy matching
There are multiple types of user intentions when matching
1.  User has in mind words or abbreviations of a term
2.  User is unsure of spelling or exact phrasing and is exploring using character guesses in the filter input
3.  User is attempting to narrow existing displayed results using any random characters within the displayed row

Commander has chosen to focus only on intention #1.  Therefore less than 2 letter consecutive intra word matches are ignored.
This appears to typically be better when filtering very long lists of thousands of items such as eclipse commands or large project files.

## Ranking
In contrast to other typical fuzzy matching command palletes, Commander does not use unlimited ranking scale.
There are only 4 levels of ranking from very strong to weak.  This was done to present some order to the found items making it easier to visually assess the information and locate items of interest.
Therefore, within each of the 4 ranking categories items are alphabetically sorted.

## User intention
Unlike other fuzzy matchers, `Commander` attempts to take into account the user intention where possible.
For example, terms separated by a `space` are considered to be literal words and fuzzy matching is not used.
A trailing `space` can be used to specify intent for literal matching of a single filter term and a leading `space` will indicate that the following term should be used as acronym only matching.
Also options to specify how fuzzy a match might also be considered.

## Software
* Plugin is built using Java 8 features.  Minimum Eclipse is therefore Neon
* Java 8 streams are utilized to parallelize the matching algorithm.  Each row is scored on a thread.
* RxJava is used to debounce the input.  All matching and scoring is done in background off the UI thread.

## Building Plugin
* Install Eclipse Committers edition which has the required eclipse SDK bundled.

