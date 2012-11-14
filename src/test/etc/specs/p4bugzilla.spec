#
# Copyright (c) 2012, Warwick Hunter. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification, 
# are permitted provided that the following conditions are met:
# 
#  1. Redistributions of source code must retain the above copyright notice, this list 
#     of conditions and the following disclaimer.
#
#  2. Redistributions in binary form must reproduce the above copyright notice, this 
#     list of conditions and the following disclaimer in the documentation and/or other 
#     materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
# EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
# OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
# SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT 
# OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
# HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
# TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
# EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

%define who Warwick Hunter

Summary:       Perforce to Bugzilla Bridge
Name:          p4bugzilla
Version:       1.1
Release:       1
License:       Apache 2.0
Vendor:        %{who} himself
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
