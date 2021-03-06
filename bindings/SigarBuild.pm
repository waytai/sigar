package SigarBuild;

use strict;
use Exporter;
use File::Basename qw(basename);
use File::Copy qw(copy);
use File::Spec ();

use vars qw(@ISA @EXPORT);
@ISA = qw(Exporter);
@EXPORT = qw(cppflags ldflags libs os src inline_src);

sub flags {
    my $os = lc $^O;
    my $is_win32 = 0;
    my (@cppflags, @ldflags, @libs);
    if ($os =~ /(win32)/) {
        $os = $1;
        $is_win32 = 1;
        @cppflags = ('-DWIN32');
        @libs = qw(kernel32 user32 advapi32 ws2_32 netapi32 shell32 pdh version);
    }
    elsif ($os =~ /(linux)/) {
        $os = $1;
    }
    elsif ($os =~ /(hpux)/) {
        $os = $1;
        @libs = qw(nsl nm);
    }
    elsif ($os =~ /(aix)/) {
        $os = $1;
        @libs = qw(odm cfg perfstat);
    }
    elsif ($os =~ /(solaris)/) {
        $os = $1;
        @libs = qw(nsl socket kstat);
    }
    elsif ($os =~ /(darwin)/) {
        $os = $1;
        my(@sdks) = reverse sort </Developer/SDKs/MacOSX10.*.sdk>;
        my $sdk;
        if (@sdks == 0) {
            die
              "Xcode Developer Tools not installed\n".
              "Download from http://developer.apple.com/technology/xcode.html";
        }
        else {
            #print "Available SDKs...\n(*) " . join("\n    ", @sdks) . "\n";
            $sdk = $sdks[0];
        }
        @cppflags = ('-DDARWIN',
                     "-I/Developer/Headers/FlatCarbon -isysroot $sdk");
        @ldflags = ("-Wl,-syslibroot,$sdk",
                    '-framework CoreServices',
                    '-framework IOKit');
        if (-e "/usr/local/libproc.h") {
            push @cppflags, '-DDARWIN_HAS_LIBPROC_H';
        }
    }
    elsif ($os =~ /bsd/) {
        $os = 'darwin';
        @libs = qw(kvm);
    }

    push @cppflags,
      '-I../../include',
      "-I../../src/os/$os";

    unless ($is_win32) {
        push @cppflags, '-U_FILE_OFFSET_BITS';
    }

    my(@src) = (<../../src/*.c>, <../../src/os/$os/*.c>);

    return {
        is_win32 => $is_win32,
        os => $os,
        libs => \@libs,
        cppflags => \@cppflags,
        ldflags => \@ldflags,
        src => \@src,
    };
}

#perl -Mlib=.. -MSigarBuild -e cppflags
sub cppflags {
    print join ' ', @{ flags()->{cppflags} };
}

sub ldflags {
    print join ' ', @{ flags()->{ldflags} };
}

sub libs {
    print join ' ', @{ flags()->{libs} };
}

sub os {
    print flags()->{os};
}

sub src {
    print join ' ', @{ flags()->{src} };
}

sub inline_src {
    my $stdout = @_ ? 0 : 1;
    my $flags = shift || flags();
    my $src = $flags->{src};
    my $dir = $flags->{build_dir} || $ARGV[0];
    my(@files);
    #unlink symlinks incase of nfs shared dir...
    for my $file (grep { -l } <*.c>) {
        unlink $file;
    }
    for my $file (@$src) {
        my $cf = basename $file;
        #sigar.c -> libsigar.c else
        #sigar.o and perl Sigar.o clash on case insensitive filesystems
        $cf = 'libsigar.c' if $cf eq 'sigar.c';
        if ($dir) {
            $cf = join '/', $dir, $cf;
            $file = File::Spec->rel2abs($file);
        }
        push @files, $cf;
        if ($flags->{is_win32}) {
            copy($file, $cf);
        }
        else {
            symlink($file, $cf) unless -e $cf;
        }
    }
    if ($stdout) {
        print join ' ', @files;
    }
    else {
        return @files;
    }
}

1;
__END__
