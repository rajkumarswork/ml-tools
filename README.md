ML Tools
========

## What
Command-line tool that exports datasets to ml-readable formats
- BigQuery tables to SvmLite, LabeledPoint, TfRecords files or GCStorage objects
- Lightweight wrapper around XGBoost that helps with training and testing

## Installing
### With brew
```
brew tap rkumar/test
brew install ml-tools
```

### From Source
```
sbt assembly
./scripts/ml-tools
```

## Usage
```
Usage: ml-tools [export|test|train] options...

Options:

  -h, --help      Show help message
  -v, --version   Show version of this program

Subcommand: export (alias: ex)
  -f, --format  <arg>     Choices: svm, lp, tf
  -h, --hashsize  <arg>
  -i, --input  <arg>
  -l, --label  <arg>
  -o, --output  <arg>
      --help              Show help message

Subcommand: train (alias: tr)
  -h, --hashsize  <arg>
  -i, --input  <arg>
  -l, --label  <arg>
  -m, --model  <arg>
      --help              Show help message

Subcommand: test (alias: te)
  -h, --hashsize  <arg>
  -i, --input  <arg>
  -l, --label  <arg>
  -m, --model  <arg>
      --help              Show help message
```

## Examples

#### Exporting from BQTable to GCStorage
```
ml-tools export -f svm -i project:dataset.click_table -l click -o gs://datasets//click_train.svm
```
Note: Since the hashsize was not specified, it will be calculated

#### Training from GCStorage and saving model to GCStorage
```
ml-tools train -i gs://datasets/click_train.svm -m gs://models/click.xgb
```

#### Evaluating a svm dataset stored in GCStorage (XValidation)
```
ml-tools test -i gs://datasets/click_train.svm
```
