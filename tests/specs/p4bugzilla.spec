%something defined here

Summary:       Perforce to Bugzilla Bridge
Name:          p4bugzilla
Version:       1.1
Release:       1
License:       Apache 2.0
Vendor:        Warwick Hunter
Packager:      w.hunter@computer.org
Provides:      %{name}-%{version}-%{release}
Requires:      jdk >= 1.5
BuildRequires: jdk >= 1.5
URL:           https://sites.google.com/site/warwickhunter
Source0:       %{name}-%{version}.tar.gz
Group:         Applications/Daemons
BuildRoot:     %{_tmppath}/%{name}
BuildArch:     noarch
AutoReqProv:   no
Prefix:        /usr

%description
A bridge between Perforce and Bugzilla. It takes p4 check-in comments and adds them to bugs.

%prep
%setup -q

%build
ant clean jar

%install
rm -rf $RPM_BUILD_ROOT
install -d -m 755 $RPM_BUILD_ROOT/opt/p4bugzilla-%{version}
for jar in lib/*.jar build/p4bugzilla.jar; do
    install -m 444 $jar $RPM_BUILD_ROOT/opt/p4bugzilla-%{version}/`basename $jar`
done
install -d -m 755 $RPM_BUILD_ROOT/etc/init.d
install -D -m 444 p4bugzilla.conf $RPM_BUILD_ROOT/etc
install -D -m 755 p4bugzilla $RPM_BUILD_ROOT/etc/init.d

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%dir /opt/p4bugzilla-%{version}
/opt/p4bugzilla-%{version}/*
%config /etc/init.d/p4bugzilla
%config /etc/p4bugzilla.conf

%post
chkconfig --add p4bugzilla

%preun
if [ "$1" = "0" ]; then
    chkconfig --del p4bugzilla
fi

%changelog
* Tue Jul 13 2010 Warwick Hunter
- 1.0-0 Initial build.
