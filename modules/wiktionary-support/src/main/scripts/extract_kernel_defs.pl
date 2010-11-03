#!/usr/bin/perl -w
use strict;
use utf8;

use constant false => 0;
use constant true  => 1;

my $numArgs = $#ARGV + 1;

if ($numArgs != 3) {
  print STDERR "Usage: extract_kernel_defs.pl lang kernel fr_extract\n";
  print STDERR "  Where: lang in fr, en, de\n";
  exit -1;
}

my $lang = $ARGV[0];
my $kernelFile = $ARGV[1];
my $extractFile = $ARGV[2];

my $lcode = "\\#fra\\|";

if ($lang eq "en") {
  $lcode = "\\#eng\\|";
} elsif ($lang eq "de") {
  $lcode = "\\#deu\\|";
}

my %kernel;
load_kernel();
extract_kernel_defs();
display_unavailable_kernel_entries();


sub load_kernel {
  open K_FH, "<",  "$kernelFile" or die $1;
  my $i = 0;
  
    while(<K_FH>) {
        chomp;
        $i++;
        my $w = $_;
        $kernel{$w} = 1;
        # print STDOUT "Comment: $_\n";
    }
}


sub extract_kernel_defs {
  my $fname = $extractFile;
  my $i=0;
  
    open ED_FH, "<",  "$fname" or die $1;
    open XED_FH, ">$fname.kdefs";
    my $current_entry = "";
    my $ignoring_current_entry = true;
    
    while(<ED_FH>) {
        chomp;
        $i++;
        if (/^-O-\s*($lcode)(.*)$/) {
          my $w = $2;
          if ($kernel{$w}) {
            $ignoring_current_entry = false;
            $current_entry = $w;
            $kernel{$w} = 2;
            print XED_FH "-O- $1$2\n";
          } else {
            $ignoring_current_entry = true;
            $current_entry = "";
          }
        } elsif (/-O-.*/) {
            $ignoring_current_entry = true;
            $current_entry = "";
        } elsif (/^\s*-D- \#pos\|(.*)$/) {
          if (! $ignoring_current_entry) {
            my $pos = $1;
            print XED_FH "  -pos- $pos\n";
          }
        } elsif (/^\s*-D- \#def\|(.*)$/) {
            if (! $ignoring_current_entry) {
            my $def = $1;
            print XED_FH "  -def- $def\n";
          }
        }
    }
    
    close XED_FH;
    close ED_FH;
}

sub display_unavailable_kernel_entries {
  foreach my $key (keys(%kernel )) {
    if ($kernel{$key} == 1) {
      print STDERR "unavailable kernel entry : \"$key\"\n";
    }
  }
}