# Eclipse Commander
Provides command palette [KAVI](https://github.com/dakaraphi/kavi) interfaces exceeding capabilities of typical sublime like command palettes

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

# KAVI implemented features for all interfaces 
 
## Term Matching

### Multi column
Default match will search across columns for matches.
![multi column](/readme-images/multi-column-match.gif)
### Specific column
Columns can be selected individual for matches by using a `,` to separate the column filters.
![specific column](/readme-images/specific-column-match.gif)
### Literal 
A space preceding filter text will cause the following text to be matched literal instead of fuzzy.
![literal column](/readme-images/literal-match.gif) 
### Fuzzy multi word out of order
Contiguous characters matched using a fuzzy strategy that attempts to match words in any order.
A space separating words will force matching of the literal words also allowing for out of order matces.
![fuzzy column](/readme-images/fuzzy-out-of-order-match.gif)
### Quality filtering
One or two letters will not match in the middle of words.  This is done to prevent a long tail of low ranking matches.
![quality](/readme-images/quality-match.gif)
### Acronym
Fuzzy matching also will attempt to match by acronym
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

## Selections
### Single select
### Range select
### Inverse select
### All select/deselect
### Implied selections

## Navigation
### List paging

## Working
### Recent
### Favorites
### Export/Import preferences
### Across workspaces

## Context Actions
### Favorites
### Sort

## Modes and selection states

# Commander
## Columns
## Launcher
# Finder
## Columns
##
